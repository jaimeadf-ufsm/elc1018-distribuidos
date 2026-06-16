package CausalMulticast;

import java.util.HashMap;
import java.util.Map;

/**
 * A matriz de relógios associa a cada participante o relógio vetorial que
 * representa o que ele já recebeu dos demais. A linha do próprio processo
 * registra seu conhecimento direto, enquanto as outras linhas registram o que
 * ele sabe sobre o conhecimento dos demais.
 */
public class MatrixClock {
    /** Relógio vetorial de cada participante, indexado pelo seu id. */
    private final Map<String, VectorClock> matrix;

    /** Cria uma matriz vazia. */
    public MatrixClock() {
        this.matrix = new HashMap<>();
    }

    /**
     * Cria uma cópia de outra matriz.
     *
     * @param other matriz a copiar
     */
    public MatrixClock(MatrixClock other) {
        this.matrix = new HashMap<>();

        for (Map.Entry<String, VectorClock> entry : other.matrix.entrySet()) {
            this.matrix.put(entry.getKey(), new VectorClock(entry.getValue()));
        }
    }

    /**
     * @param myId participante (linha)
     * @return o relógio vetorial da linha, criado vazio se ainda não existir
     */
    public synchronized VectorClock get(String myId) {
        return matrix.computeIfAbsent(myId, k -> new VectorClock());
    }

    /**
     * @param myId    participante (linha)
     * @param theirId participante (coluna)
     * @return a contagem na posição indicada
     */
    public synchronized int get(String myId, String theirId) {
        return get(myId).get(theirId);
    }

    /**
     * Substitui o relógio vetorial da linha do participante.
     *
     * @param myId    participante (linha)
     * @param theirVc novo relógio vetorial da linha
     */
    public synchronized void set(String myId, VectorClock theirVc) {
        matrix.put(myId, theirVc);
    }

    /**
     * Remove a linha do participante (usado quando ele sai do grupo).
     *
     * @param myId participante (linha) a remover
     */
    public synchronized void remove(String myId) {
        this.matrix.remove(myId);
    }

    /**
     * Incrementa um relógio da matriz.
     *
     * @param myId    participante (linha)
     * @param theirId participante (coluna)
     */
    public synchronized void increment(String myId, String theirId) {
        get(myId).increment(theirId);
    }
}