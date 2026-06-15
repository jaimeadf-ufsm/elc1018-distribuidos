package CausalMulticast;

/**
 * Um processo participante do grupo, identificado pelo par ip:porta.
 */
public class Participant implements Comparable<Participant> {
    private final String ip;
    private final int port;
    private boolean disabled;

    public Participant(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.disabled = false;
    }

    /** @return endereço IP */
    public String getIp() {
        return ip;
    }

    /** @return porta */
    public int getPort() {
        return port;
    }

    /** @return identificador */
    public String getId() {
        return ip + ":" + port;
    }

    /** @return {@code true} se o participante saiu do grupo */
    public synchronized boolean isDisabled() {
        return disabled;
    }

    /** Marca o participante como inativo. */
    public synchronized void disable() {
        this.disabled = true;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Participant that = (Participant) o;

        if (port != that.port) return false;

        return ip.equals(that.ip);
    }

    @Override
    public int compareTo(Participant other) {
        return this.getId().compareTo(other.getId());
    }

    @Override
    public String toString() {
        return getId();
    }
}