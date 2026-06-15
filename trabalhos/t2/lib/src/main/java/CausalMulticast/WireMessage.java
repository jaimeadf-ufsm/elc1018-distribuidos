package CausalMulticast;

import java.io.*;

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

    public String getSender() {
        return sender;
    }

    public VectorClock getVC() {
        return vc;
    }

    public String getContent() {
        return content;
    }

    public int getSequence() {
        return vc.get(sender);
    }

    @Override
    public String toString() {
        return sender + "[" + vc.get(sender) + "]";
    }
}