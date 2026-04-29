package elc1018.grpc.chat.common;

import com.google.protobuf.Timestamp;
import elc1018.grpc.chat.protos.ChatMessage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ChatMessageFormatter {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    public static String formatChatMessage(ChatMessage message) {
        Timestamp timestamp = message.getTimestamp();
        Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());

        String time = TIME_FORMATTER.format(instant);
        String from = message.getFrom();
        String content = message.getContent();

        return "%s <%s>: %s".formatted(time, from, content);
    }
}
