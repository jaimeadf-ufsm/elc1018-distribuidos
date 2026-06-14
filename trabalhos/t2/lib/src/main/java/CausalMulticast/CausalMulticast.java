package CausalMulticast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CausalMulticast {
    private final ICausalMulticast client;

    private final Participant self;
    private final Set<Participant> participants;

    private final MatrixClock mc;
    private final List<WireMessage> deliveryBuffer;
    private final List<WireMessage> stabilityBuffer;

    private final MessageSender sender;
    private final MessageReceiver receiver;

    private final DiscoveryService discovery;

    private EventListener eventListener;

    public CausalMulticast(String ip, Integer port, ICausalMulticast client) {
        this.client = client;

        this.self = new Participant(ip, port);
        this.participants = new HashSet<>();
        this.participants.add(this.self);

        this.mc = new MatrixClock();
        this.deliveryBuffer = new ArrayList<>();
        this.stabilityBuffer = new ArrayList<>();

        this.sender = new MessageSender();
        this.receiver = new MessageReceiver(port, this::onMessageReceived);

        this.discovery = new DiscoveryService(self, this::onParticipantDiscovered);

        this.eventListener = null;

        this.mc.increment(self.getId(), self.getId());

        this.receiver.start();
        this.discovery.start();
    }

    public synchronized void mcsend(String message, ICausalMulticast cliente) {
        WireMessage msg = new WireMessage(self.getId(), new VectorClock(mc.get(self.getId())), message);

        for (Participant participant : participants) {
            Envelope envelope = new Envelope(participant, msg, this::onEnvelopeDispatched);

            if (eventListener != null) {
                eventListener.onEnvelope(envelope);
            } else {
                envelope.dispatch();
            }
        }

        mc.increment(self.getId(), self.getId());
    }

    public synchronized void intercept(EventListener listener) {
        this.eventListener = listener;
    }
    
    public synchronized void close() {
        receiver.stop();
        discovery.stop();
    }

    private synchronized void onMessageReceived(WireMessage message) {
        if (isMessageNewer(message)) {
            mc.update(message.getSenderId(), message.getSenderClock());
        }

        System.err.printf("[INFO] mensagem %s recebida.\n", message);

        deliveryBuffer.add(message);
        stabilityBuffer.add(message);

        attemptDelivery();
        attemptDiscard();
    }

    private synchronized void onParticipantDiscovered(Participant participant) {
        if (participants.contains(participant)) {
            return;
        }

        participants.add(participant);

        // Será que eu preciso receber o vetor também pela descoberta?
        mc.increment(self.getId(), participant.getId());

        // Bloquear entrada de novos membros se eu já tiver recebido mensagens
        // de outros membros?
        // As mensagens terão já sido descartadas.

        System.err.printf("[INFO] processo %s descoberto.\n", participant);
    }

    private synchronized void onEnvelopeDispatched(Envelope envelope) {
        if (envelope.getRecipient().equals(self)) {
            onMessageReceived(envelope.getMessage());
        } else {
            sender.send(envelope.getRecipient(), envelope.getMessage());
        }
    }

    private synchronized void attemptDelivery() {
        List<WireMessage> toRemove = new java.util.ArrayList<>();

        do {
            toRemove.clear();

            for (WireMessage buffered : deliveryBuffer) {
                if (isMessageDeliverable(buffered)) {
                    toRemove.add(buffered);

                    if (!this.self.getId().equals(buffered.getSenderId())) {
                        mc.increment(self.getId(), buffered.getSenderId());
                    }

                    try {
                        client.deliver(buffered.getContent());
                    } catch (Exception e) {
                        System.err.printf("[ERROR] ocorreu um erro no processamento da mensagem %s: %s\n", buffered, e.getMessage());
                    }

                    System.err.printf("[INFO] mensagem %s entregue.\n", buffered);
                }
            }

            deliveryBuffer.removeAll(toRemove);
        } while (!toRemove.isEmpty());
    }


    public synchronized void attemptDiscard() {
        List<WireMessage> toRemove = new java.util.ArrayList<>();

        for (WireMessage buffered : stabilityBuffer) {
            if (isMessageStable(buffered)) {
                toRemove.add(buffered);
                System.err.printf("[INFO] mensagem %s descartada.\n", buffered);
            }
        }

        stabilityBuffer.removeAll(toRemove);
    }

    private synchronized boolean isMessageNewer(WireMessage message) {
        VectorClock messageVc = message.getSenderClock();

        return messageVc.get(message.getSenderId()) > mc.get(message.getSenderId(), message.getSenderId());
    }
    
    private synchronized boolean isMessageDeliverable(WireMessage message) {
        VectorClock theirVc = message.getSenderClock();

        for (String id : theirVc.keys()) {
            // Caso eu não tenha descoberto um outro processo que
            // o remetente já recebeu mensagem, ele terá X e eu terei -1.
            // Portanto, a mensagem não é entregável.

            if (theirVc.get(id) > mc.get(self.getId(), id)) {
                return false;
            }
        }

        // Caso eu tenha descoberto um processo que o remetente ainda não
        // conhece, ele nunca caíra nessa comparação. 
        // Isso não importa, pois é como se ele tivesse -1 para esse processo, e 
        // e eu terei X, o que torna sempre entregável. -1 <= X sempre.

        return true;
    }

    private synchronized boolean isMessageStable(WireMessage message) {
        String theirId = message.getSenderId();
        VectorClock theirVc = message.getSenderClock();

        for (Participant participant : participants) {
            if (theirVc.get(theirId) > mc.get(participant.getId(), theirId)) {
                return false;
            }
        }

        return true;
    }

    static interface EventListener {
        void onEnvelope(Envelope envelope);
        void onMessageReceived(WireMessage message);
        void onMesssageDelivered(WireMessage message);
        void onMessageDiscarded(WireMessage message);
    }
}
