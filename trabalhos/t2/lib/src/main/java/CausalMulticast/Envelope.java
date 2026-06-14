package CausalMulticast;

public class Envelope {
    private final Participant recipient;
    private final WireMessage message;
    private final Listener listener;

    public Envelope(Participant recipient, WireMessage message, Listener listener) {
        this.recipient = recipient;
        this.message = message;
        this.listener = listener;
    }

    public Participant getRecipient() {
        return recipient;
    }

    public WireMessage getMessage() {
        return message;
    }

    public void dispatch() {
        listener.onEnvelopeDispatched(this);
    }

    static interface Listener {
        void onEnvelopeDispatched(Envelope envelope);
    }
}
