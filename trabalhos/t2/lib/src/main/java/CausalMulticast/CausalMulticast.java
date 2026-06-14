package CausalMulticast;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CausalMulticast {
    private final ICausalMulticast client;

    private final Participant self;
    private final Set<Participant> participants;

    private final MatrixClock mc;

    private final MessageSender sender;
    private final MessageReceiver receiver;

    private final DiscoveryService discovery;

    public CausalMulticast(String ip, Integer port, ICausalMulticast client) {
        this.client = client;

        this.self = new Participant(ip, port);
        this.participants = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.participants.add(this.self);

        this.mc = new MatrixClock();

        this.sender = new MessageSender();
        this.receiver = new MessageReceiver(port, this::onMessageReceived);

        this.discovery = new DiscoveryService(self, this::onParticipantDiscovered);

        this.receiver.start();
        this.discovery.start();
    }

    public void mcsend(String message, ICausalMulticast cliente) {
        WireMessage msg = new WireMessage(self.getId(), mc.get(self.getId()), message);

        for (Participant participant : participants) {
            if (participant.getId().equals(self.getId())) {
                continue;
            }

            sender.send(participant, msg);
        }

        mc.increment(self.getId(), self.getId());
    }

    
    public void close() {
        receiver.stop();
        discovery.stop();
    }

    private void onMessageReceived(WireMessage message) {
        client.deliver(message.getContent());
    }

    private void onParticipantDiscovered(Participant participant) {
        if (participants.contains(participant)) {
            return;
        }

        participants.add(participant);
        System.err.printf("[INFO] %s descoberto.\n", participant);
    }
}
