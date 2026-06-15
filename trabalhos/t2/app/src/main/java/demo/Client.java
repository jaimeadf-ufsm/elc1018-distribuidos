package demo;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import CausalMulticast.*;

/**
 * Cliente interativo do multicast causal.
 *
 * Atua como o "controlador": conecta o middleware {@link CausalMulticast}, a
 * apresentação ({@link Console}) e o registro de transmissões retidas
 * ({@link PendingTransmissions}), encaminhando os eventos do middleware para a
 * tela e traduzindo os comandos do usuário em chamadas ao middleware.
 */
public class Client implements ICausalMulticast {
    private final Console console;
    private final PendingTransmissions pending = new PendingTransmissions();

    private final CausalMulticast middleware;

    public Client(String ip, int port, Console console) {
        this.console = console;
        this.middleware = new CausalMulticast(ip, port, this);
        this.middleware.intercept(new MiddlewareEvents());
    }

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

    @Override
    public void deliver(String message) {
        // A entrega à aplicação é apenas sinalizada na tela (ver onMesssageDelivered).
    }

    public void start() {
        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;

            while (running && scanner.hasNextLine()) {
                String[] parts = scanner.nextLine().split(" ", 2);

                String command = parts[0];
                String arg = parts.length > 1 ? parts[1] : "";

                switch (command) {
                    case "/enviar" -> middleware.mcsend(arg, this);
                    case "/transmitir" -> transmit(arg);
                    case "/pendente" -> console.pendingTransmissions(pending);
                    case "/ajuda" -> console.help();
                    case "/sair" -> running = false;
                    default -> console.unknownCommand(command);
                }
            }
        }

        middleware.close();
    }

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
     * faixas no formato "início-fim" (ex.: "1 3 5-7"). Tokens malformados são
     * ignorados silenciosamente.
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

    private class MiddlewareEvents extends CausalEventListener {
        @Override
        public void onTransmission(DeferredTransmission transmission) {
            int id = pending.register(transmission);
            console.transmission(id, transmission);
        }

        @Override
        public void onMessageReceived(WireMessage message) {
            console.message(message, middleware.getParticipants().values());
        }

        @Override
        public void onMessageDelivered(WireMessage message) {
            console.messageDelivered(message);
        }

        @Override
        public void onMessageDeposited(WireMessage message) {
            console.messageDeposited(message);
        }

        @Override
        public void onMessageDiscarded(WireMessage message) {
            console.messageDiscarded(message);
        }

        @Override
        public void onParticipantJoined(Participant participant) {
            console.participantJoined(participant);
        }

        @Override
        public void onParticipantLeft(Participant participant) {
            console.participantLeft(participant);
        }

        @Override
        public void onMatrixClockUpdated(MatrixClock clock) {
            console.matrixClock(clock, middleware.getParticipants().values());
        }

        @Override
        public void onBufferUpdated(List<WireMessage> buffer) {
            console.buffer(buffer);
        }
    }
}
