package CausalMulticast;

import java.util.List;

public class CausalEventListener {
    public CausalEventListener() {}

    public void onEnvelope(Envelope envelope) {
        envelope.dispatch();
    }

    public void onMatrixClockUpdated(MatrixClock clock) {}

    public void onMessageReceived(WireMessage message) {}

    public void onMesssageDelivered(WireMessage message) {}

    public void onMessageDeposited(WireMessage message) {}

    public void onMessageDiscarded(WireMessage message) {}

    public void onBufferUpdated(List<WireMessage> buffer) {}

    public void onParticipantJoined(Participant participant) {}
}
