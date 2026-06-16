package demo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import CausalMulticast.*;

/**
 * Formatação e coloração da saída no terminal.
 */
public final class Console {
    /** Largura de cada coluna na tabela da matriz de relógios. */
    private static final int CLOCK_WIDTH = 6;

    /**
     * Imprime o cabeçalho de abertura com o endereço local.
     *
     * @param ip   endereço local
     * @param port porta local
     */
    public void banner(String ip, int port) {
        System.out.println();
        System.out.println(Ansi.paint("  Causal Multicast — cliente", Ansi.BOLD, Ansi.CYAN));
        System.out.println(Ansi.paint("  endereço  ", Ansi.DIM) + ip + ":" + port);
        System.out.println();
    }

    /** Lista os comandos disponíveis. */
    public void help() {
        System.out.println(header("comandos", Ansi.BLUE));
        System.out.println(row(command("/enviar <msg>", "envia uma mensagem para o grupo")));
        System.out.println(row(command("/liberar <ids>", "transmite retidas (ex: 1 ou 1-3)")));
        System.out.println(row(command("/retidas", "lista as transmissões retidas")));
        System.out.println(row(command("/ajuda", "mostra esta ajuda")));
        System.out.println(row(command("/sair", "encerra o cliente")));
        System.out.println();
    }

    /**
     * Sinaliza que uma transmissão foi retida, exibindo seu id.
     *
     * @param id           id atribuído à transmissão
     * @param transmission transmissão retida
     */
    public void transmission(int id, DeferredTransmission transmission) {
        WireMessage message = transmission.getMessage();

        System.out.println(Ansi.paint(String.format("→ transmissão #%-3d %s → %s  \"%s\"", id, shortMessageId(message), shortParticipantId(transmission.getTarget()), message.getContent()), Ansi.YELLOW));
    }

    /**
     * Exibe os detalhes de uma mensagem recebida.
     *
     * @param message mensagem a exibir
     */
    public void message(WireMessage message) {
        StringBuilder block = new StringBuilder();

        block.append(header("mensagem " + shortMessageId(message), Ansi.BLUE));
        block.append("\n").append(row(field("remetente", shortParticipantId(message.getSender()))));
        block.append("\n").append(row(field("conteúdo", "\"" + message.getContent() + "\"")));
        block.append("\n").append(row(field("relógio", vectorClock(message.getVC()))));

        printBlock(block.toString());
    }

    /**
     * Exibe o conteúdo atual do buffer.
     *
     * @param buffer mensagens no buffer
     */
    public void buffer(List<WireMessage> buffer) {
        String count = buffer.isEmpty() ? "vazio" : Integer.toString(buffer.size());

        StringBuilder block = new StringBuilder(header("buffer (" + count + ")", Ansi.CYAN));

        for (WireMessage message : buffer) {
            block.append("\n").append(row(String.format("%-9s \"%s\"", shortMessageId(message), message.getContent())));
        }

        printBlock(block.toString());
    }

    /**
     * Lista as transmissões retidas aguardando liberação.
     *
     * @param pending registro das transmissões retidas
     */
    public void pendingTransmissions(PendingTransmissions pending) {
        String count = pending.empty() ? "nenhuma" : Integer.toString(pending.size());

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

    /**
     * Exibe a matriz de relógios em forma de tabela.
     *
     * @param clock        matriz de relógios a exibir
     * @param participants participantes que rotulam linhas e colunas
     */
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

    /**
     * Sinaliza que uma mensagem foi entregue à aplicação.
     *
     * @param message mensagem entregue
     */
    public void messageDelivered(WireMessage message) {
        messageAction("✓", "entregue", message, Ansi.GREEN);
    }

    /**
     * Sinaliza que uma mensagem foi depositada no buffer.
     *
     * @param message mensagem depositada
     */
    public void messageDeposited(WireMessage message) {
        messageAction("+", "depositada", message, Ansi.CYAN);
    }

    /**
     * Sinaliza que uma mensagem foi descartada do buffer.
     *
     * @param message mensagem descartada
     */
    public void messageDiscarded(WireMessage message) {
        messageAction("-", "descartada", message, Ansi.GRAY);
    }

    /**
     * Imprime uma linha de ação sobre uma mensagem.
     *
     * @param symbol  símbolo que precede a linha
     * @param verb    verbo que descreve a ação
     * @param message mensagem alvo da ação
     * @param color   cor da linha
     */
    public void messageAction(String symbol, String verb, WireMessage message, String color) {
        System.out.println(Ansi.paint(String.format("%s %-10s %-9s \"%s\"", symbol, verb, shortMessageId(message), message.getContent()), color));
    }

    /**
     * Sinaliza a entrada de um participante no grupo.
     *
     * @param participant participante que ingressou
     */
    public void participantJoined(Participant participant) {
        participantAction("+", "ingressou", participant, Ansi.MAGENTA);
    }

    /**
     * Sinaliza a saída de um participante do grupo.
     *
     * @param participant participante que saiu
     */
    public void participantLeft(Participant participant) {
        participantAction("-", "saiu", participant, Ansi.MAGENTA);
    }

    /**
     * Imprime uma linha de ação sobre um participante.
     *
     * @param symbol      símbolo que precede a linha
     * @param verb        verbo que descreve a ação
     * @param participant participante alvo da ação
     * @param color       cor da linha
     */
    public void participantAction(String symbol, String verb, Participant participant, String color) {
        System.out.println(Ansi.paint(String.format("%s %-10s %s", symbol, verb, participant), color));
    }

    /**
     * Imprime uma mensagem de aviso.
     *
     * @param text texto do aviso
     */
    public void warn(String text) {
        System.out.println(Ansi.paint("! " + text, Ansi.RED));
    }

    /**
     * Sinaliza um comando não reconhecido.
     *
     * @param command comando digitado
     */
    public void unknownCommand(String command) {
        System.out.println(Ansi.paint("! comando desconhecido: " + command, Ansi.RED)
                + Ansi.paint("  (use /ajuda)", Ansi.DIM));
    }

    /**
     * Formata um relógio vetorial como {@code porta=valor} para cada participante.
     *
     * @param vc relógio vetorial a formatar
     * @return texto formatado
     */
    private String vectorClock(VectorClock vc) {
        StringBuilder builder = new StringBuilder();

        for (String id : vc.keys()) {
            builder.append(String.format("%s=%s  ", shortParticipantId(id), clockValue(vc.get(id))));
        }

        return builder.toString().trim();
    }

    /**
     * Formata o título de um bloco.
     *
     * @param title título
     * @param color cor do título
     * @return texto formatado
     */
    private static String header(String title, String color) {
        return Ansi.paint("◆ " + title, Ansi.BOLD, color);
    }

    /**
     * Formata uma linha de conteúdo de um bloco, com a borda à esquerda.
     *
     * @param content conteúdo da linha
     * @return texto formatado
     */
    private static String row(String content) {
        return Ansi.paint("│", Ansi.DIM) + " " + content;
    }

    /**
     * Formata um comando da ajuda, com nome em destaque e descrição.
     *
     * @param name        nome do comando
     * @param description descrição do comando
     * @return texto formatado
     */
    private static String command(String name, String description) {
        return Ansi.paint(String.format("%-16s", name), Ansi.BOLD) + Ansi.paint(description, Ansi.DIM);
    }

    /**
     * Formata um par rótulo/valor.
     *
     * @param label rótulo
     * @param value valor
     * @return texto formatado
     */
    private static String field(String label, String value) {
        return String.format("%-10s %s", label, value);
    }

    /**
     * Alinha um valor à direita na largura informada.
     *
     * @param value valor a alinhar
     * @param width largura total
     * @return texto alinhado
     */
    private static String lpad(String value, int width) {
        return String.format("%" + width + "s", value);
    }

    /**
     * Formata um valor de relógio, exibindo {@code -} quando desconhecido (-1).
     *
     * @param value valor do relógio
     * @return texto formatado
     */
    private static String clockValue(int value) {
        return value == -1 ? "-" : Integer.toString(value);
    }

    /**
     * @param message mensagem
     * @return identificador curto da mensagem no formato {@code porta[sequência]}
     */
    private static String shortMessageId(WireMessage message) {
        return String.format("%s[%d]", shortParticipantId(message.getSender()), message.getSequence());
    }

    /**
     * @param participant participante
     * @return identificador curto do participante (apenas a porta)
     */
    private static String shortParticipantId(Participant participant) {
        return shortParticipantId(participant.getId());
    }

    /**
     * @param id identificador completo no formato {@code ip:porta}
     * @return identificador curto (apenas a porta)
     */
    private static String shortParticipantId(String id) {
        return id.substring(id.indexOf(":") + 1);
    }

    /**
     * Imprime um bloco com linhas em branco antes e depois.
     *
     * @param block conteúdo do bloco
     */
    private static void printBlock(String block) {
        System.out.println();
        System.out.println(block);
        System.out.println();
    }
}
