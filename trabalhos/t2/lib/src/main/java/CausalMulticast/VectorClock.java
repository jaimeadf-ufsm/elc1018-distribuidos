package CausalMulticast;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VectorClock implements Serializable {
    private final Map<String, Integer> vector;

    public VectorClock() {
        this.vector = new HashMap<>();
    }

    public VectorClock(VectorClock other) {
        this.vector = new HashMap<>(other.vector);
    }

    public synchronized int get(String id) {
        return vector.getOrDefault(id, -1);
    }

    public synchronized void increment(String id) {
        vector.put(id, get(id) + 1);
    }

    public synchronized Set<String> keys() {
        return vector.keySet();
    }
}
