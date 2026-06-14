package demo;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import CausalMulticast.*;

public class Client implements ICausalMulticast {
    private int transmissionId;
    private Map<Integer, Envelope> pendingEnvelopes;

    private CausalMulticast middleware;

    public Client(String ip, int port) {
        this.transmissionId = 1;
        this.pendingEnvelopes = new HashMap<>();


        this.middleware = new CausalMulticast(ip, port, this);
        this.middleware.intercept(new CausalEventListener() {
            @Override
            public void onEnvelope(Envelope envelope) {
                System.out.printf("transmissão #%d: %s -> %s\n", transmissionId, shortMessageId(envelope.getMessage()), shortParticipantId(envelope.getRecipient()));
                pendingEnvelopes.put(transmissionId++, envelope);
            }

            @Override
            public void onMatrixClockUpdated(MatrixClock clock) {
                System.out.println(formatMatrixClock(clock, "> "));
                System.out.println();
            }

            @Override
            public void onMessageReceived(WireMessage message) {
                System.out.println();
                System.out.println(formatMessage(message));
                System.out.println();
            }

            @Override
            public void onMesssageDelivered(WireMessage message) {
                // System.out.printf("entrega     %s\n", shortMessageId(message));
                System.out.printf("entrega %s \"%s\"\n", shortMessageId(message), message.getContent());
            }

            @Override
            public void onMessageDiscarded(WireMessage message) {
                System.out.printf("descarta %s \"%s\"\n", shortMessageId(message), message.getContent());
            }

            @Override
            public void onParticipantJoined(Participant participant) {
                System.out.printf("processo %s ingressou\n", participant);
            }
        });
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: ./gradlew :app:run --args=\"<ip> <porta>\"");
            return;
        }

        String ip = args[0];
        int port = Integer.parseInt(args[1]);

        System.err.println("Opções");
        System.err.println("  IP: " + ip);
        System.err.println("  Port: " + port);
        System.err.println();

        Client client = new Client(ip, port);
        client.start();
    }

    public void deliver(String message) {
        System.out.printf("cliente \"%s\"\n", message);
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            String[] parts = scanner.nextLine().split(" ", 2);
            

            String command = parts[0];
            String arg = parts.length > 1 ? parts[1] : "";

            if (command.equals("/enviar")) {
                System.out.println();
                middleware.mcsend(arg, this);

            } else if (command.equals("/trans")) {
                String[] components = arg.split(" ");

                Set<Integer> targetIds = new HashSet<>();
                Set<Integer> invalidIds = new HashSet<>();

                for (String idPart : components) {
                    if (idPart.contains("-")) {
                        String[] rangeParts = idPart.split("-");
                        int start = Integer.parseInt(rangeParts[0]);
                        int end = Integer.parseInt(rangeParts[1]);

                        for (int i = start; i <= end; i++) {
                            targetIds.add(i);
                        }
                    } else {
                        try {
                            targetIds.add(Integer.parseInt(idPart));
                        } catch (NumberFormatException e) {
                        }
                    }
                }

                for (Integer id : targetIds) {
                    Envelope envelope = pendingEnvelopes.get(id);

                    if (envelope != null) {
                        envelope.dispatch();
                        pendingEnvelopes.remove(id);
                    } else {
                        invalidIds.add(id);
                    }
                }

                for (Integer id : invalidIds) {
                    System.out.printf("transmissão #%d não encontrada\n", id);
                }
            } else if (command.equals("/buffer")) {
                System.out.println(formatMessageBuffer(middleware.getStabilityBuffer()));
                System.out.println();
            } else if (command.equals("/pendente")){
                for (Map.Entry<Integer, Envelope> entry : pendingEnvelopes.entrySet()) {
                    System.out.printf("- #%d: %s -> %s\n", entry.getKey(), shortMessageId(entry.getValue().getMessage()), shortParticipantId(entry.getValue().getRecipient()));
                }
                System.out.println();
            } else if (command.equals("/sair")) {
                break;
            } else {
                System.out.println("Unknown command: " + command);
            }
        }

        scanner.close();
    }

    private String formatMessage(WireMessage message) {
        StringBuilder builder = new StringBuilder();

        builder.append(String.format("> mensagem %s:\n", shortMessageId(message)));
        builder.append(String.format(">  remetente: %s\n", shortParticipantId(message.getSender())));
        builder.append(String.format(">  conteúdo:  \"%s\"\n", message.getContent()));
        builder.append(String.format(">  VC:        %s", formatVectorClock(message.getVC())));

        return builder.toString();
    }

    private String formatMessageBuffer(List<WireMessage> buffer) {
        StringBuilder builder = new StringBuilder();

        for (WireMessage message : buffer) {
            builder.append(String.format("- %s (VC: %s)\n", shortMessageId(message), formatVectorClock(message.getVC())));
        }

        return builder.toString();
    }

    private String formatMatrixClock(MatrixClock mc, String indent) {
        StringBuilder builder = new StringBuilder();
        Set<Participant> participants = middleware.getParticipants();

        builder.append(String.format("%smatriz relógio:\n", indent));

        builder.append(String.format("%s%8s |", indent, ""));
        for (Participant p : participants) {
            builder.append(String.format("%8s |", shortParticipantId(p)));
        }

        for (Participant p : participants) {
            builder.append("\n");
            builder.append(String.format("%s%8s |", indent, shortParticipantId(p)));

            for (Participant q : participants) {
                builder.append(String.format("%8s |", formatClockValue(mc.get(p.getId(), q.getId()))));
            }
        }

        return builder.toString();
    }

    private String formatVectorClock(VectorClock vc) {
        StringBuilder builder = new StringBuilder();

        Set<Participant> participants = middleware.getParticipants();

        for (Participant p : participants) {
            builder.append(String.format("%s=%s ", shortParticipantId(p), formatClockValue(vc.get(p.getId()))));
        }
    
        return builder.toString();
    }

    private String formatClockValue(int value) {
        return value == -1 ? "-" : Integer.toString(value);
    }

    private String shortMessageId(WireMessage message) {
        return String.format("%s[%d]", shortParticipantId(message.getSender()), message.getSequence());
    }

    private String shortParticipantId(Participant p) {
        return shortParticipantId(p.getId());
    }

    private String shortParticipantId(String id) {
        return "p" + id.substring(id.indexOf(":") + 1);
    }
}