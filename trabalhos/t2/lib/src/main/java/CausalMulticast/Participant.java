package CausalMulticast;

public class Participant implements Comparable<Participant> {
    private final String ip;
    private final int port;
    private boolean disabled;

    public Participant(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.disabled = false;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getId() {
        return ip + ":" + port;
    }

    public synchronized boolean isDisabled() {
        return disabled;
    }

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