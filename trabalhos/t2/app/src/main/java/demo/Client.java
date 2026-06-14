package demo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

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
                WireMessage message = envelope.getMessage();

                System.out.println(Ansi.paint(String.format("→ transmissão #%-3d %s → %s  \"%s\"",
                        transmissionId,
                        shortMessageId(message),
                        shortParticipantId(envelope.getRecipient()),
                        message.getContent()), Ansi.YELLOW));

                pendingEnvelopes.put(transmissionId++, envelope);
            }

            @Override
            public void onMatrixClockUpdated(MatrixClock clock) {
                printBlock(formatMatrixClock(clock));
                System.out.println();
            }

            @Override
            public void onMessageReceived(WireMessage message) {
                printBlock(formatMessage(message));
                System.out.println();
            }

            @Override
            public void onMesssageDelivered(WireMessage message) {
                logMessage("✓", "entregue", message, Ansi.GREEN);
            }

            @Override
            public void onMessageDeposited(WireMessage message) {
                logMessage("+", "depositada", message, Ansi.CYAN);
            }

            @Override
            public void onMessageDiscarded(WireMessage message) {
                logMessage("-", "descartada", message, Ansi.GRAY);
            }

            @Override
            public void onBufferUpdated(List<WireMessage> buffer) {
                printBlock(formatMessageBuffer(buffer));
                System.out.println();
            }

            @Override
            public void onParticipantJoined(Participant participant) {
                System.out.println(Ansi.paint("● processo " + participant + " ingressou", Ansi.MAGENTA));
            }
        });
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println(Ansi.paint("uso: ./gradlew :app:run --args=\"<ip> <porta>\"", Ansi.RED));
            return;
        }

        String ip = args[0];
        int port = Integer.parseInt(args[1]);

        printBanner(ip, port);
        printHelp();

        Client client = new Client(ip, port);
        client.start();
    }

    public void deliver(String message) {
        // System.out.printf("cliente  \"%s\"\n", message);
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            String[] parts = scanner.nextLine().split(" ", 2);


            String command = parts[0];
            String arg = parts.length > 1 ? parts[1] : "";

            if (command.equals("/enviar")) {
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
                    System.out.println(Ansi.paint(String.format("! transmissão #%d não encontrada", id), Ansi.RED));
                }
            } else if (command.equals("/buffer")) {
                System.out.println(formatMessageBuffer(middleware.getBuffer()));
                System.out.println();
            } else if (command.equals("/pendente")) {
                System.out.println(formatPendingTransmissions());
                System.out.println();
            } else if (command.equals("/ajuda")) {
                printHelp();
            } else if (command.equals("/sair")) {
                break;
            } else {
                System.out.println(Ansi.paint("! comando desconhecido: " + command, Ansi.RED)
                        + Ansi.paint("  (use /ajuda)", Ansi.DIM));
            }
        }

        scanner.close();
    }

    private static void printBanner(String ip, int port) {
        System.out.println();
        System.out.println(Ansi.paint("  Causal Multicast — cliente", Ansi.BOLD, Ansi.CYAN));
        System.out.println(Ansi.paint("  endereço  ", Ansi.DIM) + ip + ":" + port);
        System.out.println();
    }

    private static void printHelp() {
        System.out.println(header("comandos", Ansi.BLUE));
        System.out.println(row(command("/enviar <msg>", "envia uma mensagem para o grupo")));
        System.out.println(row(command("/trans <ids>", "transmite as pendentes (ex: 1 3 ou 1-3)")));
        System.out.println(row(command("/pendente", "lista as transmissões retidas")));
        System.out.println(row(command("/buffer", "mostra o buffer de mensagens")));
        System.out.println(row(command("/ajuda", "mostra esta ajuda")));
        System.out.println(row(command("/sair", "encerra o cliente")));
        System.out.println();
    }

    private void logMessage(String symbol, String verb, WireMessage message, String color) {
        System.out.println(Ansi.paint(String.format("%s %-10s %-9s \"%s\"",
                symbol, verb, shortMessageId(message), message.getContent()), color));
    }

    private String formatMessage(WireMessage message) {
        StringBuilder builder = new StringBuilder();

        builder.append(header("mensagem " + shortMessageId(message), Ansi.BLUE));
        builder.append("\n").append(row(field("remetente", shortParticipantId(message.getSender()))));
        builder.append("\n").append(row(field("conteúdo", "\"" + message.getContent() + "\"")));
        builder.append("\n").append(row(field("relógio", formatVectorClock(message.getVC()))));

        return builder.toString();
    }

    private String formatMessageBuffer(List<WireMessage> buffer) {
        StringBuilder builder = new StringBuilder();

        String count = buffer.isEmpty() ? "vazio" : Integer.toString(buffer.size());
        builder.append(header("buffer (" + count + ")", Ansi.CYAN));

        for (WireMessage message : buffer) {
            builder.append("\n").append(row(String.format("%-9s \"%s\"", shortMessageId(message), message.getContent())));
        }

        return builder.toString();
    }

    public String formatPendingTransmissions() {
        StringBuilder builder = new StringBuilder();

        String count = pendingEnvelopes.isEmpty() ? "nenhuma" : Integer.toString(pendingEnvelopes.size());
        builder.append(header("transmissões pendentes (" + count + ")", Ansi.YELLOW));

        for (Map.Entry<Integer, Envelope> entry : pendingEnvelopes.entrySet()) {
            WireMessage message = entry.getValue().getMessage();

            builder.append("\n").append(row(String.format("#%-3d %s → %s  \"%s\"",
                    entry.getKey(),
                    shortMessageId(message),
                    shortParticipantId(entry.getValue().getRecipient()),
                    message.getContent())));
        }

        return builder.toString();
    }

    private String formatMatrixClock(MatrixClock mc) {
        Set<Participant> participants = middleware.getParticipants();
        int width = 6;

        StringBuilder builder = new StringBuilder();
        builder.append(header("matriz de relógios", Ansi.MAGENTA));

        StringBuilder head = new StringBuilder(lpad("", width));
        for (Participant p : participants) {
            head.append(" ").append(Ansi.paint(lpad(shortParticipantId(p), width), Ansi.DIM));
        }
        
        builder.append("\n").append(row(head.toString()));

        for (Participant p : participants) {
            StringBuilder line = new StringBuilder();
            line.append(Ansi.paint(lpad(shortParticipantId(p), width), Ansi.DIM));

            for (Participant q : participants) {
                line.append(" ").append(lpad(formatClockValue(mc.get(p.getId(), q.getId())), width));
            }

            builder.append("\n").append(row(line.toString()));
        }

        return builder.toString();
    }

    private String formatVectorClock(VectorClock vc) {
        StringBuilder builder = new StringBuilder();

        for (Participant p : middleware.getParticipants()) {
            builder.append(String.format("%s=%s  ", shortParticipantId(p), formatClockValue(vc.get(p.getId()))));
        }

        return builder.toString().trim();
    }

    private static String header(String title, String color) {
        return Ansi.paint("◆ " + title, Ansi.BOLD, color);
    }

    private static String row(String content) {
        return Ansi.paint("│", Ansi.DIM) + " " + content;
    }

    private static String command(String name, String description) {
        return Ansi.paint(String.format("%-16s", name), Ansi.BOLD) + Ansi.paint(description, Ansi.DIM);
    }

    private static String field(String label, String value) {
        return String.format("%-10s %s", label, value);
    }

    private static String lpad(String value, int width) {
        return String.format("%" + width + "s", value);
    }

    private static void printBlock(String block) {
        System.out.println();
        System.out.println(block);
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
        return id.substring(id.indexOf(":") + 1);
    }
}
