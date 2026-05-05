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

/**
 * Classe que gerencia o servidor gRPC.
 */
public class ChatServer {
    private final ChatRoom room; // Instância da sala de chat
    private final Server server; // Instância do servidor gRPC

    public ChatServer(int port) {
        this.room = new ChatRoom();
        // Constrói o servidor na porta especificada e registra o serviço de chat
        this.server = ServerBuilder.forPort(port)
                .addService(new ChatServiceImpl(this.room))
                .build();
    }

    /**
     * Inicia o servidor e configura o desligamento automático.
     */
    public void start() throws IOException {
        server.start();
        System.out.println("[INFO] O servidor está ouvindo na porta " + server.getPort());

        // Garante que o métod stop() seja chamado se a JVM for encerrada (ex: Ctrl+C)
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    /**
     * Finaliza o servidor de forma graciosa.
     */
    public void stop() {
        try {
            System.err.println("[INFO] O servidor está sendo desligado...");
            // Tenta fechar as conexões em até 30 segundos antes de forçar o encerramento
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace(System.err);
        } finally {
            System.err.println("[INFO] O servidor foi desligado.");
        }
    }

    /**
     * Faz a thread principal esperar até que o servidor seja encerrado.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        int port = 50051; // Porta padrão

        if (args.length == 1)
            port = Integer.parseInt(args[0]);

        ChatServer server = new ChatServer(port);
        server.start();
        server.blockUntilShutdown();
    }

    /**
     * Implementação dos métodos definidos no arquivo .proto.
     */
    public static class ChatServiceImpl extends ChatServiceGrpc.ChatServiceImplBase {
        private final ChatRoom room;

        public ChatServiceImpl(ChatRoom room) {
            this.room = room;
        }

        // Operação UNÁRIA: O cliente pede registro e o servidor responde uma vez
        @Override
        public void register(User request, StreamObserver<RegisterResponse> responseObserver) {
            String username = request.getUsername();
            boolean success = this.room.register(request.getUsername());

            RegisterResponse response = RegisterResponse.newBuilder()
                    .setUsername(username)
                    .setSuccess(success)
                    .build();

            responseObserver.onNext(response); // Envia a resposta
            responseObserver.onCompleted();   // Encerra esta chamada específica
        }

        // Operação UNÁRIA: Recebe uma mensagem e confirma o recebimento (Ack)
        @Override
        public void sendMessage(ChatMessage request, StreamObserver<Ack> responseObserver) {
            boolean success = this.room.broadcast(request);
            Ack ack = Ack.newBuilder()
                    .setSuccess(success)
                    .build();

            responseObserver.onNext(ack);
            responseObserver.onCompleted();
        }

        // Operação SERVER STREAMING: O servidor mantém o canal aberto para enviar mensagens
        @Override
        public void receiveMessages(User request, StreamObserver<ChatMessage> responseObserver) {
            // Realiza o casting da stream para poder interagir com os eventos de cancelamento
            ServerCallStreamObserver<ChatMessage> serverObserver = (ServerCallStreamObserver<ChatMessage>) responseObserver;

            boolean success = room.connect(request.getUsername(), serverObserver);

            if (!success)
                responseObserver.onError(Status.INTERNAL.asRuntimeException());
        }
    }

    /**
     * Gerencia a lógica de usuários, histórico e sincronização entre threads.
     */
    public static class ChatRoom
    {
        public static final String SYSTEM_USERNAME = "sistema";
        public static final int MAX_CONCURRENT_CONNECTIONS = 1;
        public static boolean UNREGISTER_ON_DISCONNECT = true;

        private final List<ChatMessage> history; // Histórico de mensagens na memória
        private final HashMap<String, UserChannel> users; // Mapa de usuários registrados

        public ChatRoom() {
            this.history = new ArrayList<>();
            this.users = new HashMap<>();
        }

        // synchronized: Garante que apenas uma thread por vez modifique os usuários
        public synchronized boolean register(String username) {
            if (isSystem(username) || isRegistered(username)) {
                return false;
            }

            users.put(username, new UserChannel());
            alert(SystemMessageFactory.formatRegisterMessage(username));
            return true;
        }

        /**
         * Remove o usuário do sistema e fecha todas as conexões ativas dele.
         */
        public synchronized void unregister(String username) {
            if (isSystem(username) || !isRegistered(username)) return;

            UserChannel user = users.get(username);
            user.close(); // Avisa os streams do usuário que acabou
            users.remove(username);
            alert(SystemMessageFactory.formatUnregisterMessage(username));
        }

        /**
         * Vincula a stream de mensagens a um usuário.
         */
        public synchronized boolean connect(String username, ServerCallStreamObserver<ChatMessage> stream) {
            UserChannel user = users.get(username);

            if (user == null || user.size() >= MAX_CONCURRENT_CONNECTIONS) {
                return false;
            }

            // Evento disparado se o cliente cair ou fechar a conexão abruptamente
            stream.setOnCancelHandler(() -> disconnect(username, stream));

            // Envia o histórico de mensagens para o usuário que acabou de conectar
            for (ChatMessage message : history) {
                stream.onNext(message);
            }

            // Adiciona o stream à lista de conexões do usuário
            user.attach(stream);

            if (user.size() == 1)
                alert(SystemMessageFactory.createConnectionMessage(username));

            return true;
        }

        /**
         * Desconecta um stream específico sem necessariamente desregistrar o usuário.
         */
        public synchronized void disconnect(String username, StreamObserver<ChatMessage> stream) {
            UserChannel user = users.get(username);
            if (user == null) return;

            user.detach(stream);

            // Se o usuário não tem mais nenhuma conexão ativa
            // Ex.: Fechou todas as abas
            if (user.size() == 0) {
                alert(SystemMessageFactory.createDisconnectionMessage(username));
                if (UNREGISTER_ON_DISCONNECT)
                    unregister(username);
            }
        }

        /**
         * Envia uma mensagem informativa do sistema para todos.
         */
        public synchronized void alert(String content) {
            Instant now = Instant.now();
            ChatMessage message = ChatMessage.newBuilder()
                    .setFrom(SYSTEM_USERNAME)
                    .setContent(content)
                    .setTimestamp(Timestamp.newBuilder()
                            .setSeconds(now.getEpochSecond())
                            .setNanos(now.getNano())
                            .build())
                    .build();
            broadcast(message);
        }

        /**
         * Envia uma mensagem para todos os usuários registrados.
         */
        public synchronized boolean broadcast(ChatMessage message) {
            if (!isSystem(message.getFrom()) && !isRegistered(message.getFrom())) {
                return false;
            }

            // Salva a mensagem no histórico
            history.add(message);

            // Itera por todos os usuários e pede para seus canais enviarem a mensagem
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

    /**
     * Representa as conexões (streams) de um usuário específico.
     */
    public static class UserChannel {
        // Lista para armazenar as streams gRPC
        private final List<StreamObserver<ChatMessage>> streams;

        public UserChannel() {
            this.streams = Collections.synchronizedList(new ArrayList<>());
        }

        // Envia a mensagem para todas as conexões abertas deste usuário
        public void send(ChatMessage message) {
            for (StreamObserver<ChatMessage> stream : streams) {
                stream.onNext(message); // O onNext envia o dado pelo cabo de rede
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

        // Fecha todos os canais de rede deste usuário
        public void close() {
            for (StreamObserver<ChatMessage> stream : streams) {
                stream.onCompleted();
            }
        }
    }

    /**
     * Criação de textos do sistema.
     */
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