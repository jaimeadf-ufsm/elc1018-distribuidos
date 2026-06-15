package CausalMulticast;

import java.util.List;

/**
 * Observador dos eventos do middleware.
 */
public class CausalEventListener {
    public CausalEventListener() {}

    /**
     * Chamado para cada destinatário de uma mensagem enviada.
     */
    public void onTransmission(DeferredTransmission transmission) {
        transmission.dispatch();
    }

    /** Mensagem recebida da rede, antes de qualquer processamento. */
    public void onMessageReceived(WireMessage message) {}

    /** Mensagem entregue à aplicação na ordem causal. */
    public void onMessageDelivered(WireMessage message) {}

    /** Mensagem depositada no buffer. */
    public void onMessageDeposited(WireMessage message) {}

    /** Mensagem descartada do buffer por ter se tornado estável. */
    public void onMessageDiscarded(WireMessage message) {}

    /** Novo participante descoberto no grupo. */
    public void onParticipantJoined(Participant participant) {}

    /** Participante saiu do grupo. */
    public void onParticipantLeft(Participant participant) {}

    /** A matriz de relógios foi atualizada. */
    public void onMatrixClockUpdated(MatrixClock clock) {}

    /** O conteúdo do buffer mudou. */
    public void onBufferUpdated(List<WireMessage> buffer) {}
}
