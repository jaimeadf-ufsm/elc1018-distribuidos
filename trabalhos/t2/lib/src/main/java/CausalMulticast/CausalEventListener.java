package CausalMulticast;

public class CausalEventListener {
    public CausalEventListener() {}

    public void onEnvelope(Envelope envelope) {
        envelope.dispatch();
    }

    public void onMatrixClockUpdated(MatrixClock clock) {}

    public void onMessageReceived(WireMessage message) {}

    public void onMesssageDelivered(WireMessage message) {}

    public void onMessageDiscarded(WireMessage message) {}

    public void onParticipantJoined(Participant participant) {}

}
