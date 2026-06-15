package CausalMulticast;

/**
 * Contrato que a aplicação implementa para receber as mensagens entregues
 * pelo middleware na ordem causal.
 */
public interface ICausalMulticast {
    /**
     * Chamado pelo middleware quando uma mensagem está pronta para ser
     * entregue à aplicação, já respeitando a ordem causal.
     *
     * @param msg conteúdo da mensagem
     */
    public void deliver(String msg);
}