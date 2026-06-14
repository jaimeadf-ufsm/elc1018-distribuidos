package CausalMulticast;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class VectorClock implements Serializable {
    private final Map<String, Integer> vector;

    public VectorClock() {
        this.vector = new HashMap<>();
    }

    public VectorClock(VectorClock other) {
        this.vector = new HashMap<>(other.vector);
    }

    public synchronized int get(String participantId) {
        return vector.getOrDefault(participantId, -1);
    }

    public synchronized void increment(String participantId) {
        vector.put(participantId, get(participantId) + 1);
    }

    public synchronized Set<String> keys() {
        return vector.keySet();
    }
}
