package CausalMulticast;

import java.io.IOException;
import java.util.*;

/**
 * Middleware de multicast causal.
 */
public class CausalMulticast {
    /** Aplicação que recebe as mensagens na ordem causal. */
    private final ICausalMulticast client;

    /** Participante que representa este próprio processo. */
    private final Participant self;

    /** Participantes conhecidos do grupo, indexados pelo id (ip:porta). */
    private final Map<String, Participant> participants;

    /** Matriz de relógios que registra o que cada participante já recebeu. */
    private final MatrixClock mc;
   
    /** Mensagens recebidas que aguardam entrega ou ainda não são estáveis. */
    private final List<WireMessage> buffer;

    /** Serviço auxiliar para enviar mensagens a um participante por UDP. */
    private final MessageSender sender;
    
    /** Serviço auxiliar para ouvir mensagens UDP recebidas em um porta. */
    private final MessageReceiver receiver;

    /** Serviço auxiliar para broadcast de entradas e saídas de participantes. */
    private final DiscoveryService discovery;

    /** Observador dos eventos internos do middleware. */
    private EventListener listener;

    /** Indica que o grupo está formado e não aceita mais novos participantes. */
    private boolean locked;

    /**
     * Cria o middleware, inicia a escuta de mensagens e a descoberta de
     * participantes.
     *
     * @param ip     endereço local deste participante
     * @param port   porta local usada para receber mensagens
     * @param client aplicação que receberá as mensagens entregues
     */
    public CausalMulticast(String ip, Integer port, ICausalMulticast client) {
        this.client = client;

        this.self = new Participant(ip, port);
        this.participants = new TreeMap<>();
        this.participants.put(self.getId(), self);

        this.mc = new MatrixClock();
        this.buffer = new ArrayList<>();

        this.sender = new MessageSender();
        this.receiver = new MessageReceiver(port, this::onMessageReceived);

        this.discovery = new DiscoveryService(self, this::onDiscoveryMessage);

        this.listener = new EventListener() { };

        this.locked = false;

        this.mc.increment(self.getId(), self.getId());

        this.receiver.start();
        this.discovery.start();
    }

    /**
     * Envia uma mensagem para todos os participantes do grupo. A mensagem é
     * entregue imediatamente a este processo e, para os demais, encaminhada
     * como {@link DeferredTransmission} (podendo ser retida e liberada depois).
     *
     * @param message conteúdo a enviar
     * @param cliente aplicação local, que recebe a entrega imediata
     */
    public synchronized void mcsend(String message, ICausalMulticast cliente) {
        // Cria a mensagem com o vetor de relógio atual antes de incrementar
        WireMessage msg = new WireMessage(self.getId(), new VectorClock(mc.get(self.getId())), message);

        // Avança o relógio local para refletir este envio
        mc.increment(self.getId(), self.getId());
        listener.onMatrixClockUpdated(new MatrixClock(mc));

        // Deposita no buffer e tenta entrega local imediata
        onMessageReceived(msg);

        listener.onMessageDelivered(msg);
        cliente.deliver(message);

        // Encaminha para cada participante remoto ativo
        for (Participant participant : participants.values()) {
            if (participant.equals(self) || participant.isDisabled()) {
                continue;
            }

            DeferredTransmission transmission = new DeferredTransmission(participant, msg, this::onTransmissionDispatched);

            listener.onTransmission(transmission);
        }
    }

    /**
     * Registra um observador dos eventos internos do middleware (recepção,
     * entrega, descarte, transmissões retidas, etc.).
     *
     * @param listener observador a ser notificado
     */
    public synchronized void intercept(EventListener listener) {
        this.listener = listener;
    }

    /** Encerra a escuta de mensagens e a descoberta de participantes. */
    public synchronized void close() {
        receiver.stop();
        discovery.stop();
    }

    /** @return o participante que representa este processo */
    public synchronized Participant getSelf() {
        return self;
    }

    /** @return cópia dos participantes conhecidos, indexados pelo id */
    public synchronized Map<String, Participant> getParticipants() {
        return new TreeMap<>(participants);
    }

    /** @return cópia da matriz de relógios atual */
    public synchronized MatrixClock getMatrixClock() {
        return new MatrixClock(mc);
    }

    /** @return cópia das mensagens atualmente no buffer */
    public synchronized List<WireMessage> getMessageBuffer() {
        return new ArrayList<>(buffer);
    }

    /**
     * Percorre o buffer entregando todas as mensagens cujos requisitos causais
     * já estão satisfeitos, repetindo enquanto novas entregas forem possíveis.
     */
    private synchronized void attemptDelivery() {
        boolean repeat = true;

        while (repeat) {
            repeat = false;

            // Repete enquanto houver novas entregas possíveis em uma mesma varredura
            for (WireMessage buffered : buffer) {
                if (isMessageDeliverable(buffered)) {
                    repeat = true;

                    // Incrementa a nossa visão sobre o remetente somente para
                    // mensagens externas, pois mensagens próprias ja são
                    // contabilizadas no envio.
                    if (!this.self.getId().equals(buffered.getSender())) {
                        mc.increment(self.getId(), buffered.getSender());
                        listener.onMatrixClockUpdated(new MatrixClock(mc));
                    }

                    listener.onMessageDelivered(buffered);

                    try {
                        client.deliver(buffered.getContent());
                    } catch (Exception e) {
                        System.err.printf("[ERROR] ocorreu um erro no processamento da mensagem %s: %s\n", buffered,
                                e.getMessage());
                    }
                }
            }
        }
    }

    /** Remove do buffer as mensagens que já se tornaram estáveis. */
    public synchronized void attemptDiscard() {
        List<WireMessage> toRemove = new java.util.ArrayList<>();

        // Coleta separadamente para não modificar o buffer durante a iteração
        for (WireMessage buffered : buffer) {
            if (isMessageStable(buffered)) {
                toRemove.add(buffered);
            }
        }

        // Descarta as mensagens estáveis
        buffer.removeAll(toRemove);

        for (WireMessage discarded : toRemove) {
            listener.onMessageDiscarded(discarded);
        }

        if (!toRemove.isEmpty()) {
            listener.onMessageBufferUpdated(new ArrayList<>(buffer));
        }
    }

    /**
     * @param message mensagem a verificar
     * @return {@code true} se a mensagem é mais nova do que a última conhecida do
     *         remetente
     */
    private synchronized boolean isMessageNewer(WireMessage message) {
        return message.getSequence() > mc.get(message.getSender(), message.getSender());
    }

    /**
     * Verifica se a mensagem pode ser entregue à aplicação: deve ser a próxima 
     * do remetente e todas as mensagens das quais ela depende causalmente já
     * devem ter sido recebidas.
     *
     * @param message mensagem a verificar
     * @return {@code true} se a mensagem está pronta para entrega
     */
    private synchronized boolean isMessageDeliverable(WireMessage message) {
        String theirId = message.getSender();
        VectorClock theirVc = message.getVC();

        // Verifica se é a próxima mensagem do remetente:
        // VC[sender] == MC[self][sender] + 1

        // Isso é necessário, porque o algoritmo de estabilidade sempre
        // envia a sequência atual da mensagem no VC[sender] e não
        // a sequência de seu requisito causal.
        // Exemplo:
        // Na primeira mensagem, o emissor envia VC[sender] = 0,
        // porém seu requisito causal é -1.
        if (message.getSequence() != mc.get(self.getId(), theirId) + 1) {
            return false;
        }

        // Verifica se já recebeu todas as mensagens causais anteriores:
        // VC[X] <= MC[self][X] para todo X != sender
        for (String id : theirVc.keys()) {
            if (id.equals(theirId)) {
                continue;
            }

            // Caso eu não tenha descoberto um outro processo que
            // o remetente já recebeu mensagem, ele terá X e eu terei -1.
            // Portanto, a mensagem não é entregável.
            if (theirVc.get(id) > mc.get(self.getId(), id)) {
                return false;
            }
        }

        // Caso eu tenha descoberto um processo que o remetente ainda não
        // conhece, ele nunca caíra nessa comparação.
        // Porém, isso não importa, pois é como se ele tivesse -1 para esse
        // processo, e e eu terei X, o que torna sempre entregável.

        return true;
    }

    /**
     * Verifica se a mensagem é estável, ou seja, se todos os participantes
     * ativos já a receberam e portanto ela pode ser descartada do buffer.
     *
     * @param message mensagem a verificar
     * @return {@code true} se a mensagem é estável
     */
    private synchronized boolean isMessageStable(WireMessage message) {
        // Verifica se:
        // VC[sender] <= MC[X][sender] para todo X

        String theirId = message.getSender();
        VectorClock theirVc = message.getVC();

        // Ou seja, se
        // VC[sender] > MC[X][sender] para algum X
        // então a mensagem não é estável.
        for (Participant participant : participants.values()) {
            if (participant.isDisabled()) {
                continue;
            }

            if (theirVc.get(theirId) > mc.get(participant.getId(), theirId)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Adiciona um participante recém-descoberto. Ignora a inclusão após o
     * envio das primeiras mensagens, quando o grupo já está fixado.
     *
     * @param participant participante a adicionar
     */
    private synchronized void addParticipant(Participant participant) {
        // Evita registrar participantes duplicados
        Participant stored = participants.get(participant.getId());

        if (stored == null) {
            if (locked) {
                System.err.printf("[AVISO] participante %s não pode ser adicionado após envio de mensagens.\n",
                        participant);
                return;
            }

            participants.put(participant.getId(), participant);
            listener.onParticipantJoined(participant);
            listener.onMatrixClockUpdated(new MatrixClock(mc));
        }
    }

    /**
     * Marca um participante como desativado.
     *
     * @param id id do participante que saiu
     */
    private synchronized void removeParticipant(String id) {
        Participant participant = participants.get(id);

        if (participant != null && !participant.isDisabled()) {
            participant.disable();

            // Remove a linha do participante da matriz para que a estabilidade
            // não dependa mais dele. A coluna, no entanto, é mantida para
            // preservar a ordenação causal.
            mc.remove(id);
            listener.onParticipantLeft(participant);
            listener.onMatrixClockUpdated(new MatrixClock(mc));

            // Reavalia o descarte, pois a saída de um participante pode tornar
            // mensagens estáveis.
            attemptDiscard();
        }
    }

    /**
     * Trata mensagens de descoberta, adicionando (HELLO) ou removendo (BYE)
     * participantes.
     *
     * @param message mensagem de descoberta recebida
     */
    private synchronized void onDiscoveryMessage(DiscoveryMessage message) {
        Participant other = new Participant(message.getSenderIp(), message.getSenderPort());

        if (message.isHello()) {
            addParticipant(other);
        } else if (message.isBye()) {
            removeParticipant(other.getId());
        }
    }

    /**
     * Ponto de entrada de toda mensagem (própria ou da rede): atualiza a
     * matriz de relógios, deposita no buffer e dispara entrega e descarte.
     *
     * @param message mensagem recebida
     */
    private synchronized void onMessageReceived(WireMessage message) {
        // Impede que novos participantes entrem após a primeira mensagem
        this.locked = true;

        listener.onMessageReceived(message);

        Participant sender = participants.get(message.getSender());

        if (sender == null) {
            System.err.printf("[AVISO] mensagem recebida de participante desconhecido %s\n", message.getSender());
            return;
        }

        if (sender.isDisabled()) {
            System.err.printf("[AVISO] mensagem recebida de participante que já saiu %s\n", message.getSender());
            return;
        }

        // Deposita no buffer antes de tentar entregar, garantindo que a mensagem
        // esteja disponível para as verificações de ordem causal
        buffer.add(message);
        listener.onMessageDeposited(message);
        listener.onMessageBufferUpdated(new ArrayList<>(buffer));

        // Atualiza o conhecimento local sobre o histórico do remetente
        if (isMessageNewer(message)) {
            mc.set(message.getSender(), message.getVC());
            listener.onMatrixClockUpdated(new MatrixClock(mc));
        }

        // Tenta entregar mensagens em ordem causal e descartar as estáveis
        attemptDelivery();
        attemptDiscard();
    }

    /**
     * Efetiva uma transmissão liberada, entregando localmente se o destino for
     * este processo, ou envia pela rede caso contrário.
     *
     * @param transmission transmissão a efetivar
     */
    private synchronized void onTransmissionDispatched(DeferredTransmission transmission) {
        // Se o destino for este processo, processa localmente em vez de usar a rede
        if (transmission.getTarget().equals(self)) {
            onMessageReceived(transmission.getMessage());
        } else {
            try {
                sender.send(transmission.getTarget(), transmission.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Observador de eventos do middleware.
     */
    public interface EventListener {
        /**
         * Chamado para cada destinatário de uma mensagem enviada. A implementação
         * padrão despacha a transmissão imediatamente; sobrescrever permite retê-la.
         *
         * @param transmission transmissão pendente para o destinatário
         */
        default void onTransmission(DeferredTransmission transmission) {
            transmission.dispatch();
        }

        /**
         * Mensagem recebida da rede, antes de qualquer processamento.
         *
         * @param message mensagem recebida
         */
        default void onMessageReceived(WireMessage message) {
        }

        /**
         * Mensagem entregue à aplicação na ordem causal.
         *
         * @param message mensagem entregue
         */
        default void onMessageDelivered(WireMessage message) {
        }

        /**
         * Mensagem depositada no buffer.
         *
         * @param message mensagem depositada
         */
        default void onMessageDeposited(WireMessage message) {
        }

        /**
         * Mensagem descartada do buffer por ter se tornado estável.
         *
         * @param message mensagem descartada
         */
        default void onMessageDiscarded(WireMessage message) {
        }

        /**
         * Novo participante descoberto no grupo.
         *
         * @param participant participante que ingressou
         */
        default void onParticipantJoined(Participant participant) {
        }

        /**
         * Participante saiu do grupo.
         *
         * @param participant participante que saiu
         */
        default void onParticipantLeft(Participant participant) {
        }

        /**
         * A matriz de relógios foi atualizada.
         *
         * @param clock cópia da matriz de relógios atual
         */
        default void onMatrixClockUpdated(MatrixClock clock) {
        }

        /**
         * O conteúdo do buffer mudou.
         *
         * @param buffer cópia das mensagens atualmente no buffer
         */
        default void onMessageBufferUpdated(List<WireMessage> buffer) {
        }
    }
}
