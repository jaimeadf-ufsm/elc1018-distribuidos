package CausalMulticast;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

/**
 * Descoberta dinâmica de participantes via multicast UDP. Anuncia
 * periodicamente a presença deste processo ({@code HELLO}) e escuta os
 * anúncios dos demais, notificando o middleware sobre entradas e saídas.
 */
class DiscoveryService {
    private final String multicastIp;
    private final int multicastPort;
    private final Participant self;

    private final EventListener listener;

    private boolean running;

    /** Cria o serviço no grupo multicast padrão ({@code 230.0.0.1:4446}). */
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

    /** Inicia, em threads próprias, o anúncio periódico e a escuta de anúncios. */
    public void start() {
        this.running = true;

        new Thread(this::broadcastLoop).start();
        new Thread(this::listenLoop).start();
    }

    /** Interrompe os laços, enviando {@code BYE}. */
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

    /** Receptor das mensagens de descoberta recebidas. */
    public interface EventListener {
        void onDiscoveryMessage(DiscoveryMessage message);
    }
}
