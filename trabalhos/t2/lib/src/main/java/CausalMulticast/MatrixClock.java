package CausalMulticast;

import java.util.HashMap;
import java.util.Map;

class MatrixClock {
    private final Map<String, VectorClock> matrix;

    public MatrixClock() {
        this.matrix = new HashMap<>();
    }

    public VectorClock get(String senderId) {
        return matrix.getOrDefault(senderId, new VectorClock());
    }

    public int get(String senderId, String participantId) {
        return matrix.getOrDefault(senderId, new VectorClock()).get(participantId);
    }

    public void increment(String senderId, String participantId) {
        if (!matrix.containsKey(senderId)) {
            matrix.put(senderId, new VectorClock());
        }

        matrix.get(senderId).increment(participantId);
    }
}