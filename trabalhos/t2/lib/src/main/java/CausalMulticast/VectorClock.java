package CausalMulticast;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * O relógio vetorial conta, por participante, quantas mensagens já foram
 * observadas dele.
 */
public class VectorClock implements Serializable {
    private final Map<String, Integer> vector;

    /** Cria um relógio vazio. */
    public VectorClock() {
        this.vector = new HashMap<>();
    }

    /** Cria uma cópia independente de outro relógio. */
    public VectorClock(VectorClock other) {
        this.vector = new HashMap<>(other.vector);
    }

    /**
     * @param id participante
     * @return relógio do participante, ou {@code -1} se ainda desconhecido
     */
    public synchronized int get(String id) {
        return vector.getOrDefault(id, -1);
    }

    /** 
     * Incrementa a contagem do participante informado.
     * 
     * @param id participante
     * */
    public synchronized void increment(String id) {
        vector.put(id, get(id) + 1);
    }

    /** @return os participantes presentes neste relógio */
    public synchronized Set<String> keys() {
        return vector.keySet();
    }
}
