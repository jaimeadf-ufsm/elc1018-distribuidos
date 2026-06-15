package CausalMulticast;

import java.io.Serial;
import java.io.Serializable;

public class DiscoveryMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final String HELLO = "HELLO";
    private static final String BYE = "BYE";

    private final String kind;

    private final String senderIp;
    private final int senderPort;

    private DiscoveryMessage(String kind, Participant self) {
        this.kind = kind;
        this.senderIp = self.getIp();
        this.senderPort = self.getPort();
    }

    public String getSenderIp() {
        return this.senderIp;
    }

    public int getSenderPort() {
        return senderPort;
    }

    public boolean isHello() {
        return kind.equals(HELLO);
    }

    public boolean isBye() {
        return kind.equals(BYE);
    }

    public static DiscoveryMessage createHelloMessage(Participant self) {
        return new DiscoveryMessage(HELLO, self);
    }

    public static DiscoveryMessage createByeMessage(Participant self) {
        return new DiscoveryMessage(BYE, self);
    }
}
