package CausalMulticast;

import java.util.HashMap;
import java.util.Map;

public class MatrixClock {
    private final Map<String, VectorClock> matrix;

    public MatrixClock() {
        this.matrix = new HashMap<>();
    }

    public MatrixClock(MatrixClock other) {
        this.matrix = new HashMap<>();

        for (Map.Entry<String, VectorClock> entry : other.matrix.entrySet()) {
            this.matrix.put(entry.getKey(), new VectorClock(entry.getValue()));
        }
    }

    public synchronized VectorClock get(String myId) {
        return matrix.computeIfAbsent(myId, k -> new VectorClock());
    }

    public synchronized int get(String myId, String theirId) {
        return get(myId).get(theirId);
    }

    public synchronized void set(String myId, VectorClock theirVc) {
        matrix.put(myId, theirVc);
    }

    public synchronized void remove(String myId) {
        this.matrix.remove(myId);
    }

    public synchronized void increment(String myId, String theirId) {
        get(myId).increment(theirId);
    }
}