package CausalMulticast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

class DiscoveryService {
    private final String multicastIp;
    private final int multicastPort;

    private final Participant self;
    private final Listener listener;

    private boolean running; 

    public DiscoveryService(Participant self, Listener listener) {
        this("230.0.0.1", 4446, self, listener);
    }

    public DiscoveryService(String multicastIp, int multicastPort, Participant self, Listener listener) {
        this.multicastIp = multicastIp;
        this.multicastPort = multicastPort;
        this.self = self;
        this.listener = listener;

        this.running = false;
    }

    public void start() {
        this.running = true;

        new Thread(this::sendHelloLoop).start();
        new Thread(this::listenHelloLoop).start();
    }

    public void stop() {
        this.running = false;
    }

    @SuppressWarnings("deprecation")
    private void listenHelloLoop() {
        try (MulticastSocket socket = new MulticastSocket(multicastPort)) {
            InetAddress group = InetAddress.getByName(multicastIp);

            socket.joinGroup(group); 

            byte[] buffer = new byte[256];

            while (this.running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                String[] parts = message.split(":");
                
                String senderIp = parts[0];
                int senderPort = Integer.parseInt(parts[1]);

                listener.onParticipantDiscovered(new Participant(senderIp, senderPort));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

      private void sendHelloLoop() {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress group = InetAddress.getByName(multicastIp);

            while (this.running) {
                String message = self.getId();
                byte[] data = message.getBytes();

                DatagramPacket packet = new DatagramPacket(
                    data,
                    data.length,
                    group,
                    multicastPort
                );

                socket.send(packet);
                
                Thread.sleep(500);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static interface Listener {
        void onParticipantDiscovered(Participant participant);
    }
}
