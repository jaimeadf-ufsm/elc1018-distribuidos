package CausalMulticast;

/**
 * Envio de uma mensagem a um destinatário que pode ser adiado. Permite reter a
 * transmissão e liberá-la depois com {@link #dispatch()}.
 */
public class DeferredTransmission {
    private final Participant target;
    private final WireMessage message;
    private final Listener listener;

    public DeferredTransmission(Participant target, WireMessage message, Listener listener) {
        this.target = target;
        this.message = message;
        this.listener = listener;
    }

    /** @return participante de destino */
    public Participant getTarget() {
        return target;
    }

    /** @return mensagem a transmitir */
    public WireMessage getMessage() {
        return message;
    }

    /** Efetiva a transmissão, notificando o middleware. */
    public void dispatch() {
        listener.onTransmissionDispatched(this);
    }

    /** Receptor notificado quando a transmissão é efetivada. */
    public interface Listener {
        void onTransmissionDispatched(DeferredTransmission transmission);
    }
}
