package CausalMulticast;

import java.io.*;

/**
 * Mensagem que trafega na rede, carregando o id do remetente, o relógio vetorial no
 * momento do envio e o conteúdo.
 */
public class WireMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String sender;
    private final VectorClock vc;

    private final String content;

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

    @Override
    public String toString() {
        return sender + "[" + vc.get(sender) + "]";
    }
}