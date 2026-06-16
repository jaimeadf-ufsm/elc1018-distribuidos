package CausalMulticast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

/** Escuta uma porta UDP e entrega as mensagens recebidas ao middleware. */
class MessageReceiver {
    /** Porta UDP escutada. */
    private final int port;

    /** Receptor das mensagens recebidas. */
    private final Listener listener;

    /** Indica que o laço de recepção está ativo. */
    private boolean running;

    /**
     * Cria o receptor.
     *
     * @param port     porta UDP a escutar
     * @param listener receptor das mensagens recebidas
     */
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

    /** Laço que recebe pacotes UDP, desserializa e os repassa ao receptor. */
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
        /**
         * Chamado a cada mensagem recebida.
         *
         * @param message mensagem recebida
         */
        void onMessageReceived(WireMessage message);
    }
}
