package CausalMulticast;

/**
 * Envio de uma mensagem a um destinatário que pode ser adiado. Permite reter a
 * transmissão e liberá-la depois com {@link #dispatch()}.
 */
public class DeferredTransmission {
    /** Participante de destino. */
    private final Participant target;

    /** Mensagem a transmitir. */
    private final WireMessage message;

    /** Receptor notificado quando a transmissão é efetivada. */
    private final Listener listener;

    /**
     * Cria uma transmissão adiável.
     *
     * @param target   participante de destino
     * @param message  mensagem a transmitir
     * @param listener receptor notificado quando a transmissão for efetivada
     */
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
        /**
         * Chamado quando a transmissão é liberada.
         *
         * @param transmission transmissão efetivada
         */
        void onTransmissionDispatched(DeferredTransmission transmission);
    }
}
