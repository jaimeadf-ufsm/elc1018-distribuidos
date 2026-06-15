package CausalMulticast;

import java.io.Serial;
import java.io.Serializable;

/**
 * Mensagem de descoberta divulgada por multicast: {@code HELLO} anuncia a
 * presença de um participante e {@code BYE} anuncia sua saída.
 */
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

    /** @return IP do participante que enviou a mensagem */
    public String getSenderIp() {
        return this.senderIp;
    }

    /** @return porta do participante que enviou a mensagem */
    public int getSenderPort() {
        return senderPort;
    }

    /** @return {@code true} se for um anúncio de entrada */
    public boolean isHello() {
        return kind.equals(HELLO);
    }

    /** @return {@code true} se for um anúncio de saída */
    public boolean isBye() {
        return kind.equals(BYE);
    }

    /** Cria uma mensagem de entrada para o participante informado. */
    public static DiscoveryMessage createHelloMessage(Participant self) {
        return new DiscoveryMessage(HELLO, self);
    }

    /** Cria uma mensagem de saída para o participante informado. */
    public static DiscoveryMessage createByeMessage(Participant self) {
        return new DiscoveryMessage(BYE, self);
    }
}
