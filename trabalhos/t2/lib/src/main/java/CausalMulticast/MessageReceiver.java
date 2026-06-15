package CausalMulticast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

/** Escuta uma porta UDP e entrega as mensagens recebidas ao middleware. */
class MessageReceiver {
    private final int port;
    private final Listener listener;

    private boolean running;

    public MessageReceiver(int port, Listener listener) {
        this.port = port;
        this.listener = listener;

        this.running = false;
    }

    /** Inicia, em uma thread própria, o laço de recepção de mensagens. */
    public void start() {
        this.running = true;

        new Thread(this::listenMessageLoop).start();
    }

    /** Interrompe o laço de recepção. */
    public void stop() {
        this.running = false;
    }

    private void listenMessageLoop() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] buffer = new byte[65507];

            while (this.running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                byte[] received = Arrays.copyOf(packet.getData(), packet.getLength());

                try {
                    listener.onMessageReceived((WireMessage) Serialization.convertFromBytes(received));
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Receptor das mensagens recebidas da rede. */
    public static interface Listener {
        void onMessageReceived(WireMessage message);
    }
}
