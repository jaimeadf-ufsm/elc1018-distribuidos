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
    /** IP do grupo multicast usado na descoberta. */
    private final String multicastIp;

    /** Porta do grupo multicast usado na descoberta. */
    private final int multicastPort;

    /** Participante local anunciado ao grupo. */
    private final Participant self;

    /** Receptor das mensagens de descoberta recebidas. */
    private final EventListener listener;

    /** Socket multicast, mantido para poder ser fechado no encerramento. */
    private MulticastSocket socket;

    /** Indica que os laços de anúncio e escuta estão ativos. */
    private boolean running;

    /**
     * Cria o serviço no grupo multicast padrão ({@code 230.0.0.1:4446}).
     *
     * @param self          participante local a ser anunciado
     * @param eventListener receptor das mensagens de descoberta recebidas
     */
    public DiscoveryService(Participant self, EventListener eventListener) {
        this("230.0.0.1", 4446, self, eventListener);
    }

    /**
     * Cria o serviço em um grupo multicast específico.
     *
     * @param multicastIp   IP do grupo multicast
     * @param multicastPort porta do grupo multicast
     * @param self          participante local a ser anunciado
     * @param listener      receptor das mensagens de descoberta recebidas
     */
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

        if (socket != null) {
            socket.close();
        }
    }

    /**
     * Envia uma mensagem de descoberta ao grupo multicast.
     *
     * @param message mensagem a difundir
     */
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

    /** Laço que escuta os anúncios do grupo e os repassa ao receptor. */
    @SuppressWarnings("deprecation")
    private void listenLoop() {
        try (MulticastSocket socket = new MulticastSocket(multicastPort)) {
            this.socket = socket;

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
            // O fechamento do socket em stop() interrompe o receive() com uma
            // exceção.
            if (this.running) {
                e.printStackTrace();
            }
        }
    }

    /** Laço que anuncia {@code HELLO} periodicamente e envia {@code BYE} ao encerrar. */
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
        /**
         * Chamado a cada mensagem de descoberta recebida.
         *
         * @param message mensagem recebida
         */
        void onDiscoveryMessage(DiscoveryMessage message);
    }
}
