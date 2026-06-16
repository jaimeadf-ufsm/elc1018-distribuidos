package CausalMulticast;

/**
 * Um processo participante do grupo, identificado pelo par ip:porta.
 */
public class Participant implements Comparable<Participant> {
    /** Endereço IP. */
    private final String ip;

    /** Porta. */
    private final int port;

    /** Indica que o participante saiu do grupo. */
    private boolean disabled;

    /**
     * Cria um participante ativo.
     *
     * @param ip   endereço IP
     * @param port porta
     */
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

    /**
     * Dois participantes são iguais quando têm o mesmo IP e porta.
     *
     * @param o objeto a comparar
     * @return {@code true} se representam o mesmo participante
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Participant that = (Participant) o;

        if (port != that.port) return false;

        return ip.equals(that.ip);
    }

    /**
     * Ordena os participantes pelo id ({@code ip:porta}).
     *
     * @param other participante a comparar
     * @return resultado da comparação dos ids
     */
    @Override
    public int compareTo(Participant other) {
        return this.getId().compareTo(other.getId());
    }

    /** @return o identificador ({@code ip:porta}) */
    @Override
    public String toString() {
        return getId();
    }
}