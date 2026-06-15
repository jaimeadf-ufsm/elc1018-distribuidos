package demo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import CausalMulticast.*;

/**
 * Camada de apresentação do cliente: concentra toda a formatação e coloração
 * da saída no terminal. Não guarda estado nem toma decisões — apenas recebe os
 * dados já prontos e os imprime de forma legível.
 */
public final class Console {
    private static final int CLOCK_WIDTH = 6;

    public void banner(String ip, int port) {
        System.out.println();
        System.out.println(Ansi.paint("  Causal Multicast — cliente", Ansi.BOLD, Ansi.CYAN));
        System.out.println(Ansi.paint("  endereço  ", Ansi.DIM) + ip + ":" + port);
        System.out.println();
    }

    public void help() {
        System.out.println(header("comandos", Ansi.BLUE));
        System.out.println(row(command("/enviar <msg>", "envia uma mensagem para o grupo")));
        System.out.println(row(command("/trans <ids>", "transmite as pendentes (ex: 1 3 ou 1-3)")));
        System.out.println(row(command("/pendente", "lista as transmissões retidas")));
        System.out.println(row(command("/buffer", "mostra o buffer de mensagens")));
        System.out.println(row(command("/ajuda", "mostra esta ajuda")));
        System.out.println(row(command("/sair", "encerra o cliente")));
        System.out.println();
    }

    public void transmissionRetained(int id, DeferredTransmission transmission) {
        WireMessage message = transmission.getMessage();

        System.out.println(Ansi.paint(String.format("→ transmissão #%-3d %s → %s  \"%s\"", id, shortMessageId(message), shortParticipantId(transmission.getTarget()), message.getContent()), Ansi.YELLOW));
    }

    public void message(WireMessage message, Collection<Participant> participants) {
        StringBuilder block = new StringBuilder();

        block.append(header("mensagem " + shortMessageId(message), Ansi.BLUE));
        block.append("\n").append(row(field("remetente", shortParticipantId(message.getSender()))));
        block.append("\n").append(row(field("conteúdo", "\"" + message.getContent() + "\"")));
        block.append("\n").append(row(field("relógio", vectorClock(message.getVC(), participants))));

        printBlock(block.toString());
    }

    public void buffer(List<WireMessage> buffer) {
        String count = buffer.isEmpty() ? "vazio" : Integer.toString(buffer.size());

        StringBuilder block = new StringBuilder(header("buffer (" + count + ")", Ansi.CYAN));

        for (WireMessage message : buffer) {
            block.append("\n").append(row(String.format("%-9s \"%s\"", shortMessageId(message), message.getContent())));
        }

        printBlock(block.toString());
    }

    public void pendingTransmissions(PendingTransmissions pending) {
        String count = pending.isEmpty() ? "nenhuma" : Integer.toString(pending.size());

        StringBuilder block = new StringBuilder(header("transmissões pendentes (" + count + ")", Ansi.YELLOW));

        for (Map.Entry<Integer, DeferredTransmission> entry : pending.entries()) {
            WireMessage message = entry.getValue().getMessage();

            block.append("\n").append(row(String.format("#%-3d %s → %s  \"%s\"",
                    entry.getKey(),
                    shortMessageId(message),
                    shortParticipantId(entry.getValue().getTarget()),
                    message.getContent())));
        }

        printBlock(block.toString());
    }

    public void matrixClock(MatrixClock clock, Collection<Participant> participants) {
        StringBuilder block = new StringBuilder(header("matriz de relógios", Ansi.MAGENTA));
        StringBuilder head = new StringBuilder(lpad("", CLOCK_WIDTH));

        for (Participant p : participants) {
            head.append(" ").append(Ansi.paint(lpad(shortParticipantId(p), CLOCK_WIDTH), Ansi.DIM));
        }

        block.append("\n").append(row(head.toString()));

        for (Participant p : participants) {
            StringBuilder line = new StringBuilder(Ansi.paint(lpad(shortParticipantId(p), CLOCK_WIDTH), Ansi.DIM));

            for (Participant q : participants) {
                line.append(" ").append(lpad(clockValue(clock.get(p.getId(), q.getId())), CLOCK_WIDTH));
            }

            block.append("\n").append(row(line.toString()));
        }

        printBlock(block.toString());
    }

    public void messageDelivered(WireMessage message) {
        messageAction("✓", "entregue", message, Ansi.GREEN);
    }

    public void messageDeposited(WireMessage message) {
        messageAction("+", "depositada", message, Ansi.CYAN);
    }

    public void messageDiscarded(WireMessage message) {
        messageAction("-", "descartada", message, Ansi.GRAY);
    }

    public void messageAction(String symbol, String verb, WireMessage message, String color) {
        System.out.println(Ansi.paint(String.format("%s %-10s %-9s \"%s\"", symbol, verb, shortMessageId(message), message.getContent()), color));
    }

    public void participantJoined(Participant participant) {
        participantAction("+", "ingressou", participant, Ansi.MAGENTA);
    }

    public void participantLeft(Participant participant) {
        participantAction("-", "saiu", participant, Ansi.MAGENTA);
    }

    public void participantAction(String symbol, String verb, Participant participant, String color) {
        System.out.println(Ansi.paint(String.format("%s %-10s %s", symbol, verb, participant), color));
    }

    public void warn(String text) {
        System.out.println(Ansi.paint("! " + text, Ansi.RED));
    }

    public void unknownCommand(String command) {
        System.out.println(Ansi.paint("! comando desconhecido: " + command, Ansi.RED)
                + Ansi.paint("  (use /ajuda)", Ansi.DIM));
    }

    private String vectorClock(VectorClock vc, Collection<Participant> participants) {
        StringBuilder builder = new StringBuilder();

        for (Participant p : participants) {
            builder.append(String.format("%s=%s  ", shortParticipantId(p), clockValue(vc.get(p.getId()))));
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

    private static String clockValue(int value) {
        return value == -1 ? "-" : Integer.toString(value);
    }

    private static String shortMessageId(WireMessage message) {
        return String.format("%s[%d]", shortParticipantId(message.getSender()), message.getSequence());
    }

    private static String shortParticipantId(Participant participant) {
        return shortParticipantId(participant.getId());
    }

    private static String shortParticipantId(String id) {
        return id.substring(id.indexOf(":") + 1);
    }

    private static void printBlock(String block) {
        System.out.println();
        System.out.println(block);
        System.out.println();
    }
}
