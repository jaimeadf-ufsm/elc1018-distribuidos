package CausalMulticast;

import java.io.IOException;
import java.util.*;

public class CausalMulticast {
    private final ICausalMulticast client;

    private final Participant self;
    private final Map<String, Participant> participants;

    private final MatrixClock mc;
    private final List<WireMessage> buffer;

    private final MessageSender sender;
    private final MessageReceiver receiver;

    private final DiscoveryService discovery;

    private CausalEventListener listener;

    private boolean locked;

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

        this.listener = new CausalEventListener();

        this.locked = false;

        this.mc.increment(self.getId(), self.getId());

        this.receiver.start();
        this.discovery.start();
    }

    public synchronized void mcsend(String message, ICausalMulticast cliente) {
        WireMessage msg = new WireMessage(self.getId(), new VectorClock(mc.get(self.getId())), message);

        mc.increment(self.getId(), self.getId());
        listener.onMatrixClockUpdated(new MatrixClock(mc));

        onMessageReceived(msg);

        listener.onMessageDelivered(msg);
        cliente.deliver(message);

        for (Participant participant : participants.values()) {
            if (participant.equals(self)) {
                continue;
            }

            DeferredTransmission transmission = new DeferredTransmission(participant, msg, this::onTransmissionDispatched);

            listener.onTransmission(transmission);
        }
    }

    public synchronized void intercept(CausalEventListener listener) {
        this.listener = listener;
    }
    
    public synchronized void close() {
        receiver.stop();
        discovery.stop();
    }

    public synchronized Participant getSelf() {
        return self;
    }

    public synchronized Map<String, Participant> getParticipants() {
        return new TreeMap<>(participants);
    }

    public synchronized MatrixClock getMatrixClock() {
        return new MatrixClock(mc);
    }

    public synchronized List<WireMessage> getBuffer() {
        return new ArrayList<>(buffer);
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
                        listener.onMatrixClockUpdated(new MatrixClock(mc));
                    }

                    listener.onMessageDelivered(buffered);

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
            listener.onMessageDiscarded(discarded);
        }

        if (!toRemove.isEmpty()) {
            listener.onBufferUpdated(new ArrayList<>(buffer));
        }
    }

    private synchronized boolean isMessageNewer(WireMessage message) {
        return message.getSequence() > mc.get(message.getSender(), message.getSender());
    }
    
    private synchronized boolean isMessageDeliverable(WireMessage message) {
        String theirId = message.getSender();
        VectorClock theirVc = message.getVC();

        if (message.getSequence() - 1 != mc.get(self.getId(), theirId)) {
            return false;
        }

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
        // Isso não importa, pois é como se ele tivesse -1 para esse processo, e 
        // e eu terei X, o que torna sempre entregável. -1 <= X sempre.

        return true;
    }

    private synchronized boolean isMessageStable(WireMessage message) {
        String theirId = message.getSender();
        VectorClock theirVc = message.getVC();

        for (Participant participant : participants.values()) {
            if (theirVc.get(theirId) > mc.get(participant.getId(), theirId)) {
                return false;
            }
        }

        return true;
    }

    private synchronized void addParticipant(Participant participant) {
        Participant stored = participants.get(participant.getId());

        if (stored == null) {
            if (locked) {
                System.err.printf("[ERRO] participante %s não pode ser adicionado após envio de mensagens.\n", participant);
                return;
            }

            participants.put(participant.getId(), participant);
            listener.onParticipantJoined(participant);
        }
    }

    private synchronized void removeParticipant(String id) {
        Participant participant = participants.get(id);

        if (participant != null && !participant.isDisabled()) {
            participant.disable();
            mc.remove(id);

            listener.onParticipantLeft(participant);
        }
    }

    private synchronized void onDiscoveryMessage(DiscoveryMessage message) {
        Participant other = new Participant(message.getSenderIp(), message.getSenderPort());

        if (message.isHello()) {
            addParticipant(other);
        } else if (message.isBye()) {
            removeParticipant(other.getId());
        }
    }

    private synchronized void onMessageReceived(WireMessage message) {
        this.locked = true;

        listener.onMessageReceived(message);

        if (isMessageNewer(message)) {
            mc.set(message.getSender(), message.getVC());
            listener.onMatrixClockUpdated(new MatrixClock(mc));
        }

        buffer.add(message);

        listener.onMessageDeposited(message);
        listener.onBufferUpdated(new ArrayList<>(buffer));

        attemptDelivery();
        attemptDiscard();
    }

    private synchronized void onTransmissionDispatched(DeferredTransmission transmission) {
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
}

