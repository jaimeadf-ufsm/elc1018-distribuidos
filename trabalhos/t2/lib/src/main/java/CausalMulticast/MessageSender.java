package CausalMulticast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

class MessageSender {
    public MessageSender() {
        
    }

    public void send(Participant participant, WireMessage message) throws IOException {
        byte[] data = Serialization.convertToBytes(message);

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(participant.getIp());

            DatagramPacket packet = new DatagramPacket(
                data,
                data.length,
                address,
                participant.getPort()
            );

            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
