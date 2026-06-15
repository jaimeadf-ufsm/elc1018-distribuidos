package CausalMulticast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class CausalMulticast {
    private final ICausalMulticast client;

    private final Participant self;
    private final Set<Participant> participants;

    private final MatrixClock mc;
    private final List<WireMessage> buffer;

    private final MessageSender sender;
    private final MessageReceiver receiver;

    private final DiscoveryService discovery;

    private CausalEventListener eventListener;

    public CausalMulticast(String ip, Integer port, ICausalMulticast client) {
        this.client = client;

        this.self = new Participant(ip, port);
        this.participants = new TreeSet<>();
        this.participants.add(this.self);

        this.mc = new MatrixClock();
        this.buffer = new ArrayList<>();

        this.sender = new MessageSender();
        this.receiver = new MessageReceiver(port, this::onMessageReceived);

        this.discovery = new DiscoveryService(self, this::onParticipantDiscovered);

        this.eventListener = new CausalEventListener();

        this.mc.increment(self.getId(), self.getId());

        this.receiver.start();
        this.discovery.start();
    }

    public synchronized void mcsend(String message, ICausalMulticast cliente) {
        WireMessage msg = new WireMessage(self.getId(), new VectorClock(mc.get(self.getId())), message);

        mc.increment(self.getId(), self.getId());
        eventListener.onMatrixClockUpdated(new MatrixClock(mc));

        onMessageReceived(msg);

        eventListener.onMesssageDelivered(msg);
        cliente.deliver(message);

        for (Participant participant : participants) {
            if (participant.equals(self)) {
                continue;
            }

            Envelope envelope = new Envelope(participant, msg, this::onEnvelopeDispatched);

            eventListener.onEnvelope(envelope);
        }
    }

    public synchronized void intercept(CausalEventListener listener) {
        this.eventListener = listener;
    }
    
    public synchronized void close() {
        receiver.stop();
        discovery.stop();
    }

    public synchronized Participant getSelf() {
        return self;
    }

    public synchronized Set<Participant> getParticipants() {
        return new TreeSet<>(participants);
    }

    public synchronized MatrixClock getMatrixClock() {
        return new MatrixClock(mc);
    }

    public synchronized List<WireMessage> getBuffer() {
        return new ArrayList<>(buffer);
    }

    private synchronized void onMessageReceived(WireMessage message) {
        eventListener.onMessageReceived(message);

        if (isMessageNewer(message)) {
            mc.update(message.getSender(), message.getVC());
            eventListener.onMatrixClockUpdated(new MatrixClock(mc));
        }

        buffer.add(message);

        eventListener.onMessageDeposited(message);
        eventListener.onBufferUpdated(new ArrayList<>(buffer));

        attemptDelivery();
        attemptDiscard();
    }

    private synchronized void onParticipantDiscovered(Participant participant) {
        if (participants.contains(participant)) {
            return;
        }

        participants.add(participant);

        // Será que eu preciso receber o vetor também pela descoberta?
        // mc.increment(self.getId(), participant.getId());

        // Bloquear entrada de novos membros se eu já tiver recebido mensagens
        // de outros membros?
        // As mensagens terão já sido descartadas.

        eventListener.onParticipantJoined(participant);
    }

    private synchronized void onEnvelopeDispatched(Envelope envelope) {
        if (envelope.getRecipient().equals(self)) {
            onMessageReceived(envelope.getMessage());
        } else {
            sender.send(envelope.getRecipient(), envelope.getMessage());
        }
    }

    private synchronized void attemptDelivery() {
        boolean repeat = true;

        while (repeat) {
            repeat = false;

            for (WireMessage buffered : buffer) {
                if (isMessageDeliverable(buffered)) {
                    repeat = true;

                    if (!this.self.getId().equals(buffered.getSender())) {
                        mc.increment(self.getId(), buffered.getSender());
                        eventListener.onMatrixClockUpdated(new MatrixClock(mc));
                    }

                    eventListener.onMesssageDelivered(buffered);

                    try {
                        client.deliver(buffered.getContent());
                    } catch (Exception e) {
                        System.err.printf("[ERROR] ocorreu um erro no processamento da mensagem %s: %s\n", buffered, e.getMessage());
                    }
                }
            }
        }
    }


    public synchronized void attemptDiscard() {
        List<WireMessage> toRemove = new java.util.ArrayList<>();

        for (WireMessage buffered : buffer) {
            if (isMessageStable(buffered)) {
                toRemove.add(buffered);
            }
        }

        buffer.removeAll(toRemove);

        for (WireMessage discarded : toRemove) {
            eventListener.onMessageDiscarded(discarded);
        }

        if (!toRemove.isEmpty()) {
            eventListener.onBufferUpdated(new ArrayList<>(buffer));
        }
    }

    private synchronized boolean isMessageNewer(WireMessage message) {
        VectorClock messageVc = message.getVC();

        return messageVc.get(message.getSender()) > mc.get(message.getSender(), message.getSender());
    }
    
    private synchronized boolean isMessageDeliverable(WireMessage message) {
        String theirId = message.getSender();
        VectorClock theirVc = message.getVC();

        // Garantia de ordenação causal sobre o algoritmo de estabilidade.

        // Verifica se é a próxima mensagem do remetente:
        // VC[sender] == MC[self][sender] + 1

        // Isso é necessário, porque o algoritmo de estabilidade sempre
        // envia a sequência atual da mensagem no VC[sender] e não
        // a sequência de seu requisito causal.
        //
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

    private synchronized boolean isMessageStable(WireMessage message) {
        // Verifica se:
        // VC[sender] <= MC[X][sender] para todo X

        String theirId = message.getSender();
        VectorClock theirVc = message.getVC();

        // Ou seja, se
        // VC[sender] > MC[X][sender] para algum X
        // então a mensagem não é estável.
        for (Participant participant : participants) {
            if (theirVc.get(theirId) > mc.get(participant.getId(), theirId)) {
                return false;
            }
        }

        return true;
    }
}

