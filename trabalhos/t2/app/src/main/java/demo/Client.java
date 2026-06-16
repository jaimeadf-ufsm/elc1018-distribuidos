package demo;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import CausalMulticast.*;

/** Cliente interativo do multicast. */
public class Client implements ICausalMulticast {
    /** Saída usada para exibir o estado. */
    private final Console console;

    /** Registro das transmissões retidas aguardando liberação. */
    private final PendingTransmissions pending = new PendingTransmissions();

    /** Middleware de multicast causal. */
    private final CausalMulticast middleware;

    /**
     * Cria o cliente e inicia o middleware, registrando-se como observador dos
     * seus eventos.
     *
     * @param ip      endereço local
     * @param port    porta local
     * @param console saída usada para exibir o estado
     */
    public Client(String ip, int port, Console console) {
        this.console = console;
        this.middleware = new CausalMulticast(ip, port, this);
        this.middleware.intercept(new MiddlewareEvents());
    }

    /**
     * Ponto de entrada. Espera o endereço e a porta locais como argumentos.
     *
     * @param args {@code <ip> <porta>}
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println(Ansi.paint("uso: ./gradlew :app:run --args=\"<ip> <porta>\"", Ansi.RED));
            return;
        }

        String ip = args[0];
        int port = Integer.parseInt(args[1]);

        Console console = new Console();
        console.banner(ip, port);
        console.help();

        new Client(ip, port, console).start();
    }

    /**
     * Recebe a mensagem entregue pelo middleware. A entrega é apenas sinalizada
     * na tela (ver {@code onMessageDelivered}), por isso o corpo é vazio.
     *
     * @param message conteúdo entregue
     */
    @Override
    public void deliver(String message) {
        // A entrega à aplicação é apenas sinalizada na tela (ver onMesssageDelivered).
        System.out.println(Ansi.paint("> " + message, Ansi.DIM));
    }

    /** Lê e executa os comandos do usuário até {@code /sair}. */
    public void start() {
        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;

            while (running && scanner.hasNextLine()) {
                String[] parts = scanner.nextLine().split(" ", 2);

                String command = parts[0];
                String arg = parts.length > 1 ? parts[1] : "";

                switch (command) {
                    case "/enviar" -> middleware.mcsend(arg, this);
                    case "/liberar" -> transmit(arg);
                    case "/retidas" -> console.pendingTransmissions(pending);
                    case "/ajuda" -> console.help();
                    case "/sair" -> running = false;
                    default -> console.unknownCommand(command);
                }
            }
        }

        middleware.close();
    }

    /**
     * Libera as transmissões retidas cujos ids são indicados no argumento.
     *
     * @param spec ids das transmissões a liberar (ex.: {@code "1 3 5-7"})
     */
    private void transmit(String spec) {
        for (int id : parseIds(spec)) {
            DeferredTransmission transmission = pending.remove(id);

            if (transmission != null) {
                transmission.dispatch();
            } else {
                console.warn(String.format("transmissão #%d não encontrada", id));
            }
        }
    }

    /**
     * Interpreta uma lista de identificadores separados por espaço, aceitando
     * faixas no formato "início-fim" (ex.: "1 3 5-7"). Tokens inválidos são
     * ignorados.
     *
     * @param spec texto com os ids e faixas
     * @return ids interpretados, na ordem de leitura e sem repetições
     */
    private Set<Integer> parseIds(String spec) {
        Set<Integer> ids = new LinkedHashSet<>();

        for (String token : spec.split(" ")) {
            if (token.isBlank()) {
                continue;
            }

            try {
                if (token.contains("-")) {
                    String[] range = token.split("-");
                    int start = Integer.parseInt(range[0].trim());
                    int end = Integer.parseInt(range[1].trim());

                    for (int id = start; id <= end; id++) {
                        ids.add(id);
                    }
                } else {
                    ids.add(Integer.parseInt(token.trim()));
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                // ignora tokens inválidos
            }
        }

        return ids;
    }

    /** Ouve os eventos do middleware e os reflete no console. */
    private class MiddlewareEvents implements CausalMulticast.EventListener {
        /** Retém a transmissão e a exibe na lista de pendentes. */
        @Override
        public void onTransmission(DeferredTransmission transmission) {
            int id = pending.register(transmission);
            console.transmission(id, transmission);
        }

        /** Exibe os detalhes da mensagem recebida. */
        @Override
        public void onMessageReceived(WireMessage message) {
            console.message(message);
        }

        /** Sinaliza a entrega da mensagem à aplicação. */
        @Override
        public void onMessageDelivered(WireMessage message) {
            console.messageDelivered(message);
        }

        /** Sinaliza o depósito da mensagem no buffer. */
        @Override
        public void onMessageDeposited(WireMessage message) {
            console.messageDeposited(message);
        }

        /** Sinaliza o descarte da mensagem do buffer. */
        @Override
        public void onMessageDiscarded(WireMessage message) {
            console.messageDiscarded(message);
        }

        /** Sinaliza a entrada de um participante. */
        @Override
        public void onParticipantJoined(Participant participant) {
            console.participantJoined(participant);
        }

        /** Sinaliza a saída de um participante. */
        @Override
        public void onParticipantLeft(Participant participant) {
            console.participantLeft(participant);
        }

        /** Exibe a matriz de relógios atualizada. */
        @Override
        public void onMatrixClockUpdated(MatrixClock clock) {
            console.matrixClock(clock, middleware.getParticipants().values());
        }

        /** Exibe o conteúdo atualizado do buffer. */
        @Override
        public void onMessageBufferUpdated(List<WireMessage> buffer) {
            console.buffer(buffer);
        }
    }
}
