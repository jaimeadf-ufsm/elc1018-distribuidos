package CausalMulticast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

class WireMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String senderId;
    private final VectorClock senderVc;

    private final String content;

    public WireMessage(String senderId, VectorClock senderVc, String content) {
        this.senderId = senderId;
        this.senderVc = senderVc;
        this.content = content;
    }

    public String getSenderId() {
        return senderId;
    }

    public VectorClock getSenderClock() {
        return senderVc;
    }

    public String getContent() {
        return content;
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
        return senderId + "[" + senderVc.get(senderId) + "]";
    }

    public static WireMessage fromBytes(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);

        try (ObjectInputStream in = new ObjectInputStream(bis)) {
            return (WireMessage) in.readObject();
        }
    }
}