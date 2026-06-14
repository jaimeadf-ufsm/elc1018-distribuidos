package CausalMulticast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class WireMessage implements Serializable {
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

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(this);
        }

        return bos.toByteArray();
    }

    @Override
    public String toString() {
        return sender + "[" + vc.get(sender) + "]";
    }

    public static WireMessage fromBytes(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);

        try (ObjectInputStream in = new ObjectInputStream(bis)) {
            return (WireMessage) in.readObject();
        }
    }
}