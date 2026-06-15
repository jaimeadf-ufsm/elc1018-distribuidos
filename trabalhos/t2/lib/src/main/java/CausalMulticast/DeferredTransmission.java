package CausalMulticast;

public class DeferredTransmission {
    private final Participant target;
    private final WireMessage message;
    private final Listener listener;

    public DeferredTransmission(Participant target, WireMessage message, Listener listener) {
        this.target = target;
        this.message = message;
        this.listener = listener;
    }

    public Participant getTarget() {
        return target;
    }

    public WireMessage getMessage() {
        return message;
    }

    public void dispatch() {
        listener.onTransmissionDispatched(this);
    }

    public interface Listener {
        void onTransmissionDispatched(DeferredTransmission transmission);
    }
}
