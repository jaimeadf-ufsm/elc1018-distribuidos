package CausalMulticast;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

class VectorClock implements Serializable {
    private final Map<String, Integer> vector;

    public VectorClock() {
        this.vector = new HashMap<>();
    }

    public int get(String participantId) {
        return vector.getOrDefault(participantId, 0);
    }

    public void increment(String participantId) {
        vector.put(participantId, vector.getOrDefault(participantId, 0) + 1);
    }
}
