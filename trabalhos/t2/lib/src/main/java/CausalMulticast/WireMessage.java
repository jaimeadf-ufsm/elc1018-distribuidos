package CausalMulticast;

import java.io.*;

/**
 * Mensagem que trafega na rede, carregando o id do remetente, o relógio vetorial no
 * momento do envio e o conteúdo.
 */
public class WireMessage implements Serializable {
    /** Versão da classe para a serialização. */
    @Serial
    private static final long serialVersionUID = 1L;

    /** Id do remetente. */
    private final String sender;

    /** Relógio vetorial do remetente no momento do envio. */
    private final VectorClock vc;

    /** Conteúdo da mensagem. */
    private final String content;

    /**
     * Cria uma mensagem.
     *
     * @param senderId id do remetente
     * @param senderVc relógio vetorial do remetente no momento do envio
     * @param content  conteúdo da mensagem
     */
    public WireMessage(String senderId, VectorClock senderVc, String content) {
        this.sender = senderId;
        this.vc = senderVc;
        this.content = content;
    }

    /** @return id do remetente */
    public String getSender() {
        return sender;
    }

    /** @return relógios do remetente na hora do envio */
    public VectorClock getVC() {
        return vc;
    }

    /** @return conteúdo da mensagem */
    public String getContent() {
        return content;
    }

    /** @return número atual da mensagem */
    public int getSequence() {
        return vc.get(sender);
    }

    /** @return representação curta no formato {@code remetente[sequência]} */
    @Override
    public String toString() {
        return sender + "[" + vc.get(sender) + "]";
    }
}