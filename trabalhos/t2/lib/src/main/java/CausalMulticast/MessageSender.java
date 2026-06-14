package CausalMulticast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

class MessageSender {
    public MessageSender() {
        
    }

    public void send(Participant participant, WireMessage message) {
        byte[] data;

        try {
            data = message.toBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
