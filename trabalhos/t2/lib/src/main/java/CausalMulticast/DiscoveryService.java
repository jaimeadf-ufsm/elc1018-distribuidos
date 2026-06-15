package CausalMulticast;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

class DiscoveryService {
    private final String multicastIp;
    private final int multicastPort;
    private final Participant self;

    private final EventListener listener;

    private boolean running;

    public DiscoveryService(Participant self, EventListener eventListener) {
        this("230.0.0.1", 4446, self, eventListener);
    }

    public DiscoveryService(String multicastIp, int multicastPort, Participant self, EventListener listener) {
        this.multicastIp = multicastIp;
        this.multicastPort = multicastPort;
        this.self = self;
        this.listener = listener;

        this.running = false;
    }

    public void start() {
        this.running = true;

        new Thread(this::broadcastLoop).start();
        new Thread(this::listenLoop).start();
    }

    public void stop() {
        this.running = false;
    }

    private void broadcast(DiscoveryMessage message) {
        try {
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress group = InetAddress.getByName(multicastIp);

                byte[] data = Serialization.convertToBytes(message);

                DatagramPacket packet = new DatagramPacket(
                        data,
                        data.length,
                        group,
                        multicastPort
                );

                socket.send(packet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    private void listenLoop() {
        try (MulticastSocket socket = new MulticastSocket(multicastPort)) {
            InetAddress group = InetAddress.getByName(multicastIp);

            socket.joinGroup(group); 

            byte[] buffer = new byte[65507];

            while (this.running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                byte[] received = Arrays.copyOf(packet.getData(), packet.getLength());

                try {
                    listener.onDiscoveryMessage((DiscoveryMessage) Serialization.convertFromBytes(received));
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastLoop() {
        try {
          while (this.running) {
              broadcast(DiscoveryMessage.createHelloMessage(self));
              Thread.sleep(500);
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

        broadcast(DiscoveryMessage.createByeMessage(self));
    }

    public interface EventListener {
        void onDiscoveryMessage(DiscoveryMessage message);
    }
}
