package CausalMulticast;

import java.util.HashMap;
import java.util.Map;

class MatrixClock {
    private final Map<String, VectorClock> matrix;

    public MatrixClock() {
        this.matrix = new HashMap<>();
    }

    public synchronized VectorClock get(String myId) {
        return matrix.computeIfAbsent(myId, k -> new VectorClock());
    }

    public synchronized int get(String myId, String theirId) {
        return get(myId).get(theirId);
    }

    public synchronized void update(String myId, VectorClock theirVc) {
        matrix.put(myId, theirVc);
    }

    public synchronized void increment(String myId, String theirId) {
        get(myId).increment(theirId);
    }
}