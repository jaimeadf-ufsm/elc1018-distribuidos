package CausalMulticast;

import java.io.Serial;
import java.io.Serializable;

/**
 * Mensagem de descoberta divulgada por multicast: {@code HELLO} anuncia a
 * presença de um participante e {@code BYE} anuncia sua saída.
 */
public class DiscoveryMessage implements Serializable {
    /** Versão da classe para a serialização. */
    @Serial
    private static final long serialVersionUID = 1L;

    /** Tipo que anuncia a entrada de um participante. */
    private static final String HELLO = "HELLO";

    /** Tipo que anuncia a saída de um participante. */
    private static final String BYE = "BYE";

    /** Tipo desta mensagem ({@code HELLO} ou {@code BYE}). */
    private final String kind;

    /** IP do participante que enviou a mensagem. */
    private final String senderIp;

    /** Porta do participante que enviou a mensagem. */
    private final int senderPort;

    /**
     * Cria uma mensagem de descoberta.
     *
     * @param kind tipo da mensagem ({@code HELLO} ou {@code BYE})
     * @param self participante que envia a mensagem
     */
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

    /**
     * Cria uma mensagem de entrada para o participante informado.
     *
     * @param self participante que está entrando
     * @return mensagem {@code HELLO}
     */
    public static DiscoveryMessage createHelloMessage(Participant self) {
        return new DiscoveryMessage(HELLO, self);
    }

    /**
     * Cria uma mensagem de saída para o participante informado.
     *
     * @param self participante que está saindo
     * @return mensagem {@code BYE}
     */
    public static DiscoveryMessage createByeMessage(Participant self) {
        return new DiscoveryMessage(BYE, self);
    }
}
