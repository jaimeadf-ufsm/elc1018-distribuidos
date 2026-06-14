package CausalMulticast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CausalMulticast {
    private static final String MULTICAST_IP = "230.0.0.1";
    private static final int MULTICAST_PORT = 4446;

    private final ICausalMulticast client;

    private final Participant self;
    private final List<Participant> participants;

    private final MatrixClock mc;

    public CausalMulticast(String ip, Integer port, ICausalMulticast client) {
        this.client = client;

        this.self = new Participant(ip, port);
        this.participants = new ArrayList<>();
        this.participants.add(this.self);

        this.mc = new MatrixClock();

        new Thread(this::listenMessageLoop).start();
        new Thread(this::sendHelloLoop).start();
        new Thread(this::listenHelloLoop).start();
    }

    public void mcsend(String message, ICausalMulticast cliente) {
        WireMessage msg = new WireMessage(self.getId(), mc.get(self.getId()), message);

        for (Participant participant : participants) {
            if (participant.getId().equals(self.getId())) {
                continue;
            }

            sendTo(participant, msg);
        }

        mc.increment(self.getId(), self.getId());
    }

    private void onReceive(WireMessage message) {
        client.deliver(message.getContent());
    }

    private void sendTo(Participant participant, WireMessage message) {
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

    private void sendHelloLoop() {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress group = InetAddress.getByName(MULTICAST_IP);

            while (true) {
                String message = self.getId();
                byte[] data = message.getBytes();

                DatagramPacket packet = new DatagramPacket(
                    data,
                    data.length,
                    group,
                    MULTICAST_PORT
                );

                socket.send(packet);
                
                Thread.sleep(500);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void listenMessageLoop() {
        try (DatagramSocket socket = new DatagramSocket(self.getPort())) {
            byte[] buffer = new byte[65507];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                byte[] received = Arrays.copyOf(packet.getData(), packet.getLength());

                try {
                    onReceive(WireMessage.fromBytes(received));
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    continue;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenHelloLoop() {
        try (MulticastSocket socket = new MulticastSocket(MULTICAST_PORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST_IP);

            socket.joinGroup(group); 

            byte[] buffer = new byte[256];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                String[] parts = message.split(":");
                
                String senderIp = parts[0];
                int senderPort = Integer.parseInt(parts[1]);
                
                ensureParticipant(senderIp, senderPort);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ensureParticipant(String ip, int port) {
        for (Participant participant : participants) {
            if (participant.getIp().equals(ip) && participant.getPort() == port) {
                return;
            }
        }

        System.err.printf("[INFO] %s:%d descoberto.\n", ip, port);
        participants.add(new Participant(ip, port));
    }
}
