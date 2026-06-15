package CausalMulticast;

import java.util.List;

public class CausalEventListener {
    public CausalEventListener() {}

    public void onTransmission(DeferredTransmission transmission) {
        transmission.dispatch();
    }

    public void onMessageReceived(WireMessage message) {}

    public void onMessageDelivered(WireMessage message) {}

    public void onMessageDeposited(WireMessage message) {}

    public void onMessageDiscarded(WireMessage message) {}

    public void onParticipantJoined(Participant participant) {}

    public void onParticipantLeft(Participant participant) {}

    public void onMatrixClockUpdated(MatrixClock clock) {}

    public void onBufferUpdated(List<WireMessage> buffer) {}
}
