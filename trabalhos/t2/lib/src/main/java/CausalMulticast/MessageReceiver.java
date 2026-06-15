package CausalMulticast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

class MessageReceiver {
    private final int port;
    private final Listener listener;

    private boolean running;

    public MessageReceiver(int port, Listener listener) {
        this.port = port;
        this.listener = listener;

        this.running = false;
    }

    public void start() {
        this.running = true;

        new Thread(this::listenMessageLoop).start();
    }

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

    public static interface Listener {
        void onMessageReceived(WireMessage message);
    }
}
