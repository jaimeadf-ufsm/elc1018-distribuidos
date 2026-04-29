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

public class ChatClient {
    private final ManagedChannel channel;
    private final ChatServiceGrpc.ChatServiceStub serviceAsyncStub;
    private final ChatServiceGrpc.ChatServiceBlockingStub serviceBlockingStub;

    private final String username;

    public ChatClient(String target, String username) {
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.serviceAsyncStub = ChatServiceGrpc.newStub(this.channel);
        this.serviceBlockingStub = ChatServiceGrpc.newBlockingStub(this.channel);

        this.username = username;
    }

    public boolean register() {
        User user = User.newBuilder()
                .setUsername(username)
                .build();

        try {
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

    public void join() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        CountDownLatch disconnectLatch = new CountDownLatch(1);

        System.out.printf("Você foi conectado à sala como \"%s\"%n", username);
        System.out.println("- Digite e pressione ENTER para enviar mensagens.");
        System.out.println("- Digite \"/sair\" para sair.");
        System.out.println();

        this.receive(disconnectLatch);

        try {
            while (disconnectLatch.getCount() > 0) {
                if (System.in.available() == 0) {
                    Thread.sleep(100);
                    continue;
                }

                String content = reader.readLine();

                if (content.isBlank())
                    continue;

                if (content.trim().equalsIgnoreCase("/sair"))
                    break;

                send(content);
            }
        } catch (Exception e) {
            System.err.printf("[ERRO] Ocorreu um erro ao ler as entradas: %s%n", e.getMessage());
        }
    }

    public boolean send(String content)
    {
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
            Ack ack = serviceBlockingStub.sendMessage(message);

            if (!ack.getSuccess()) {
                System.err.println("[AVISO] O servidor não aceitou a mensagem.");
                return false;
            }

            return true;
        } catch (Exception e) {
            System.err.printf("[ERRO] Ocorreu um erro ao enviar uma mensagem: %s%n", e.getMessage());
            return false;
        }
    }

    public void receive(CountDownLatch disconnectLatch) {
        User user = User.newBuilder()
                .setUsername(username)
                .build();

        serviceAsyncStub.receiveMessages(user, new StreamObserver<ChatMessage>() {
            @Override
            public void onNext(ChatMessage value) {
                System.out.println(ChatMessageFormatter.formatChatMessage(value));
            }

            @Override
            public void onError(Throwable t) {
                System.err.printf("[ERRO] Ocorreu um erro enquanto esperava por mensagens: %s%n", t.getMessage());
                disconnectLatch.countDown();
            }

            @Override
            public void onCompleted() {
                System.err.println("[INFO] O recebimento de mensagens foi encerrado pelo servidor");
                disconnectLatch.countDown();
            }
        });
    }

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

        if (command.equals("register")) {
            if (client.register())
                client.join();
        } else if (command.equals("join")) {
            client.join();
        } else {
            System.err.printf("[ERRO] O comando \"%s\" é inválido%n", command);
        }

        client.shutdown();
    }
}
