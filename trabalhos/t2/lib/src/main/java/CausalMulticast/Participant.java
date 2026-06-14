package CausalMulticast;

class Participant {
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
}