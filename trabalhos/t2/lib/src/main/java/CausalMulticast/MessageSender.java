package CausalMulticast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/** Envia mensagens a um participante específico por unicast UDP. */
class MessageSender {
    /** Cria o emissor. */
    public MessageSender() { }

    /**
     * Serializa e envia a mensagem ao participante de destino.
     *
     * @param participant destinatário
     * @param message     mensagem a enviar
     * @throws IOException em caso de falha na serialização
     */
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
