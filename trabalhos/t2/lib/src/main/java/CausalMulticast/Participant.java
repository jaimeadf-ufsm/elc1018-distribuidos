package CausalMulticast;

public class Participant implements Comparable<Participant> {
    private final String ip;
    private final int port;

    public Participant(String ip, int port) {
        this.ip = ip;
        this.port = port;
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

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Participant that = (Participant) o;

        if (port != that.port) return false;

        return ip.equals(that.ip);
    }

    public int hashCode() {
        int result = ip.hashCode();
        result = 31 * result + port;

        return result;
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