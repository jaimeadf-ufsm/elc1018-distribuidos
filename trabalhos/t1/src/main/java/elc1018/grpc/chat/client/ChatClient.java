package elc1018.grpc.chat.client;

import com.google.protobuf.Timestamp;
import elc1018.grpc.chat.common.ChatMessageFormatter;
import elc1018.grpc.chat.protos.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * ChatClient: O "tradutor" entre o teclado do usuário e a rede gRPC.
 */
public class ChatClient {
    // A conexão física com o servidor
    private final ManagedChannel channel;

    // Stub Assíncrono: Usado para o streaming de mensagens (não trava o programa)
    private final ChatServiceGrpc.ChatServiceStub serviceAsyncStub;

    // Stub Bloqueante: Usado para operações que esperam o servidor responder
    private final ChatServiceGrpc.ChatServiceBlockingStub serviceBlockingStub;

    private final String username;

    public ChatClient(String target, String username) {
        // Constrói o canal de comunicação
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

        // Criamos os dois tipos de Stub (sync, async) para o servidor
        this.serviceAsyncStub = ChatServiceGrpc.newStub(this.channel);
        this.serviceBlockingStub = ChatServiceGrpc.newBlockingStub(this.channel);

        this.username = username;
    }

    /**
     * Tenta registrar o nome de usuário no servidor.
     * Usa o stub bloqueante porque precisamos da resposta para saber se podemos continuar.
     */
    public boolean register() {
        User user = User.newBuilder().setUsername(username).build();

        try {
            // O programa para aqui até que o servidor responda RegisterResponse
            RegisterResponse response = serviceBlockingStub.register(user);

            if (!response.getSuccess()) {
                System.err.printf("[AVISO] Não foi possível registrar o usuário \"%s\"%n", username);
                return false;
            }

            System.err.printf("[INFO] O usuário \"%s\" foi registrado com sucesso%n", username);
            return true;
        } catch (Exception e) {
            System.err.printf("[ERRO] Ocorreu um erro ao registrar o usuário: %s%n", e.getMessage());
            return false;
        }
    }

    /**
     * O loop principal da interface de usuário.
     */
    public void join() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        // CountDownLatch(1): Um cadeado que mantém o programa vivo.
        // Ele só abre quando alguém chama .countDown().
        CountDownLatch disconnectLatch = new CountDownLatch(1);

        System.out.printf("Você foi conectado à sala como \"%s\"%n", username);
        System.out.println("- Digite e pressione ENTER para enviar mensagens.");
        System.out.println("- Digite \"/sair\" para sair.");
        System.out.println();

        // Dispara a escuta de mensagens em uma thread separada (Assíncrona)
        this.receive(disconnectLatch);

        try {
            // Enquanto o cadeado estiver trancado (contagem > 0), o loop continua
            while (disconnectLatch.getCount() > 0) {
                // Checa se há algo digitado sem travar a thread (Non-blocking I/O)
                if (System.in.available() == 0) {
                    Thread.sleep(100); // Descansa a CPU por 0.1s
                    continue;
                }

                String content = reader.readLine();

                if (content.isBlank()) continue;

                if (content.trim().equalsIgnoreCase("/sair"))
                    break; // Sai do loop para encerrar o programa

                send(content); // Envia o texto para o servidor
            }
        } catch (Exception e) {
            System.err.printf("[ERRO] Ocorreu um erro ao ler as entradas: %s%n", e.getMessage());
        }
    }

    /**
     * Envia uma mensagem via RPC Unário.
     */
    public boolean send(String content) {
        Instant now = Instant.now();
        ChatMessage message = ChatMessage.newBuilder()
                .setFrom(username)
                .setContent(content)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build())
                .build();

        try {
            // Bloqueia brevemente apenas para confirmar se o servidor recebeu o pacote
            Ack ack = serviceBlockingStub.sendMessage(message);
            return ack.getSuccess();
        } catch (Exception e) {
            System.err.printf("[ERRO] Ocorreu um erro ao enviar uma mensagem: %s%n", e.getMessage());
            return false;
        }
    }

    /**
     * Abre o canal de Streaming para receber mensagens em tempo real.
     */
    public void receive(CountDownLatch disconnectLatch) {
        User user = User.newBuilder().setUsername(username).build();

        // Aqui usamos o Stub assíncrono
        serviceAsyncStub.receiveMessages(user, new StreamObserver<ChatMessage>() {
            @Override
            public void onNext(ChatMessage value) {
                // Chamado sempre que o servidor empurra uma nova mensagem no stream
                System.out.println(ChatMessageFormatter.formatChatMessage(value));
            }

            @Override
            public void onError(Throwable t) {
                // Chamado se a conexão cair ou houver erro no servidor
                System.err.printf("[ERRO] Ocorreu um erro enquanto esperava por mensagens: %s%n", t.getMessage());
                disconnectLatch.countDown(); // Abre o cadeado para encerrar o programa
            }

            @Override
            public void onCompleted() {
                // Chamado se o servidor fechar o stream educadamente
                System.err.println("[INFO] O recebimento de mensagens foi encerrado pelo servidor");
                disconnectLatch.countDown(); // Abre o cadeado para encerrar o programa
            }
        });
    }

    /**
     * Fecha a conexão com o servidor.
     */
    public void shutdown() throws InterruptedException {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 3) {
            System.err.println("Uso: ChatClient <comando> <host> <username>");
            System.err.println();
            System.err.println("Argumentos:");
            System.err.println("  comando   Ação a ser executada (\"register\" ou \"join\")");
            System.err.println("  host      Endereço do servidor alvo");
            System.err.println("  username  Seu nome de usuário");
            return;
        }

        String command = args[0];
        String target = args[1];
        String username = args[2];

        ChatClient client = new ChatClient(target, username);

        // Se o comando for 'register', ele registra e já entra na sala (join)
        // Se for 'join', ele tenta entrar direto (pressupõe que já está registrado)
        if (command.equals("register")) {
            if (client.register()) client.join();
        } else if (command.equals("join")) {
            client.join();
        }

        client.shutdown();
    }
}