package elc1018.grpc.chat.server;

import com.google.protobuf.Timestamp;
import elc1018.grpc.chat.common.ChatMessageFormatter;
import elc1018.grpc.chat.protos.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ChatServer {
    private final ChatRoom room;
    private final Server server;

    public ChatServer(int port) {
        this.room = new ChatRoom();
        this.server = ServerBuilder.forPort(port)
                .addService(new ChatServiceImpl(this.room))
                .build();
    }

    public void start() throws IOException {
        server.start();
        System.out.println("[INFO] O servidor está ouvindo na porta " + server.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public void stop() {
        try {
            System.err.println("[INFO] O servidor está sendo desligado...");
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace(System.err);
        } finally {
            System.err.println("[INFO] O servidor foi desligado.");
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        if (args.length > 1) {
            System.err.println("Uso: ChatServer [porta]");
            System.err.println();
            System.err.println("Argumentos:");
            System.err.println("  porta     A porta em que o servidor roda");
            return;
        }

        int port = 50051;

        if (args.length == 1)
            port = Integer.parseInt(args[0]);

        ChatServer server = new ChatServer(port);
        server.start();
        server.blockUntilShutdown();
    }

     public static class ChatServiceImpl extends ChatServiceGrpc.ChatServiceImplBase {
        private final ChatRoom room;

        public ChatServiceImpl(ChatRoom room) {
            this.room = room;
        }

        @Override
        public void register(User request, StreamObserver<RegisterResponse> responseObserver) {
            String username = request.getUsername();

            boolean success = this.room.register(request.getUsername());
            RegisterResponse response = RegisterResponse.newBuilder()
                    .setUsername(username)
                    .setSuccess(success)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void sendMessage(ChatMessage request, StreamObserver<Ack> responseObserver) {
            boolean success = this.room.broadcast(request);
            Ack ack = Ack.newBuilder()
                    .setSuccess(success)
                    .build();

            responseObserver.onNext(ack);
            responseObserver.onCompleted();
        }

        @Override
        public void receiveMessages(User request, StreamObserver<ChatMessage> responseObserver) {
            ServerCallStreamObserver<ChatMessage> serverObserver = (ServerCallStreamObserver<ChatMessage>) responseObserver;

            boolean success = room.connect(request.getUsername(), serverObserver);

            if (!success)
                responseObserver.onError(Status.INTERNAL.asRuntimeException());
        }
    }

    public static class ChatRoom
    {
        public static final String SYSTEM_USERNAME = "sistema";

        public static final int MAX_CONCURRENT_CONNECTIONS = 1;
        public static boolean UNREGISTER_ON_DISCONNECT = true;

        private final List<ChatMessage> history;
        private final HashMap<String, UserChannel> users;

        public ChatRoom() {
            this.history = new ArrayList<>();
            this.users = new HashMap<>();
        }

        public synchronized boolean register(String username) {
            if (isSystem(username)) {
                System.err.printf("[AVISO] O usuário \"%s\" não pode ser registrado%n", username);
                return false;
            }

            if (isRegistered(username)) {
                System.err.printf("[AVISO] O usuário \"%s\" já está registrado%n", username);
                return false;
            }

            users.put(username, new UserChannel());

            System.err.printf("[INFO] O usuário \"%s\" foi registrado com sucesso%n", username);
            alert(SystemMessageFactory.formatRegisterMessage(username));

            return true;
        }

        public synchronized void unregister(String username) {
            if (isSystem(username)) {
                System.err.printf("[AVISO] O usuário \"%s\" não pode ser desregistrado%n", username);
                return;
            }

            if (!isRegistered(username)) {
                System.err.printf("[AVISO] O usuário \"%s\" já não está registrado%n", username);
                return;
            }

            UserChannel user = users.get(username);
            user.close();

            users.remove(username);

            System.err.printf("[INFO] O usuário \"%s\" foi desregistrado com sucesso%n", username);
            alert(SystemMessageFactory.formatUnregisterMessage(username));
        }

        public synchronized boolean connect(String username, ServerCallStreamObserver<ChatMessage> stream) {
            UserChannel user = users.get(username);

            if (user == null) {
                System.err.printf("[AVISO] O usuário \"%s\" tentou se conectar sem realizar cadastro%n", username);
                return false;
            }

            if (user.size() >= MAX_CONCURRENT_CONNECTIONS) {
                System.err.printf("[AVISO] O usuário \"%s\" já atingiu o limite de conexões simultâneas%n", username);
                return false;
            }

            stream.setOnCancelHandler(() -> disconnect(username, stream));

            for (ChatMessage message : history) {
                stream.onNext(message);
            }

            user.attach(stream);
            System.err.printf("[INFO] O usuário \"%s\" se conectou (%d conexões ativas)%n", username, user.size());

            if (user.size() == 1) {
                alert(SystemMessageFactory.createConnectionMessage(username));
            }

            return true;
        }

        public synchronized void disconnect(String username, StreamObserver<ChatMessage> stream) {
            UserChannel user = users.get(username);

            if (user == null)
                return;

            user.detach(stream);
            System.err.printf("[INFO] O usuário \"%s\" se desconectou (%d conexões ativas)%n", username, user.size());

            if (user.size() == 0) {
                alert(SystemMessageFactory.createDisconnectionMessage(username));

                if (UNREGISTER_ON_DISCONNECT)
                    unregister(username);
            }
        }

        public synchronized void alert(String content) {
            Instant now = Instant.now();

            ChatMessage message = ChatMessage.newBuilder()
                    .setFrom(SYSTEM_USERNAME)
                    .setContent(content)
                    .setTimestamp(Timestamp.newBuilder()
                            .setSeconds(now.getEpochSecond())
                            .setNanos(now.getNano())
                            .build()
                    )
                    .build();

            broadcast(message);
        }

        public synchronized boolean broadcast(ChatMessage message) {
            if (!isSystem(message.getFrom()) && !isRegistered(message.getFrom())) {
                System.err.printf("[INFO] O usuário \"%s\" não está registrado para enviar mensagens%n", message.getFrom());
                return false;
            }

            history.add(message);

            for (UserChannel user : users.values()) {
                user.send(message);
            }

            System.out.println(ChatMessageFormatter.formatChatMessage(message));

            return true;
        }

        public synchronized boolean isSystem(String username) {
            return username.equals(SYSTEM_USERNAME);
        }

        public synchronized boolean isRegistered(String username) {
            return this.users.containsKey(username);
        }
    }

    public static class UserChannel {
        private final List<StreamObserver<ChatMessage>> streams;

        public UserChannel() {
            this.streams = Collections.synchronizedList(new ArrayList<>());
        }

        public void send(ChatMessage message) {
            for (StreamObserver<ChatMessage> stream : streams) {
                stream.onNext(message);
            }
        }

        public void attach(StreamObserver<ChatMessage> stream) {
            this.streams.add(stream);
        }

        public void detach(StreamObserver<ChatMessage> stream) {
            this.streams.remove(stream);
        }

        public int size() {
            return this.streams.size();
        }

        public void close() {
            for (StreamObserver<ChatMessage> stream : streams) {
                stream.onCompleted();
            }
        }
    }

    public static class SystemMessageFactory {
        public static String formatRegisterMessage(String username) {
            return "O usuário \"%s\" foi registrado".formatted(username);
        }

        public static String formatUnregisterMessage(String username) {
            return "O usuário \"%s\" foi desregistrado".formatted(username);
        }

        public static String createConnectionMessage(String username) {
            return "O usuário \"%s\" entrou na sala".formatted(username);
        }

        public static String createDisconnectionMessage(String username) {
            return "O usuário \"%s\" saiu da sala".formatted(username);
        }
    }
}
