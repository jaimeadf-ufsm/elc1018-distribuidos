package demo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import CausalMulticast.DeferredTransmission;

/**
 * Registro das transmissões retidas pelo cliente.
 */
public final class PendingTransmissions {
    /** Transmissões retidas, indexadas pelo id atribuído. */
    private final Map<Integer, DeferredTransmission> transmissions = new LinkedHashMap<>();

    /** Próximo id a ser atribuído a uma transmissão. */
    private int nextId = 1;

    /**
     * Registra uma transmissão retida.
     *
     * @param transmission transmissão a guardar
     * @return id atribuído, usado depois para liberá-la
     */
    public int register(DeferredTransmission transmission) {
        int id = nextId++;
        transmissions.put(id, transmission);

        return id;
    }

    /**
     * Remove e retorna a transmissão com o id informado.
     *
     * @param id identificador da transmissão
     * @return a transmissão, ou {@code null} se não existir
     */
    public DeferredTransmission remove(int id) {
        return transmissions.remove(id);
    }

    /** @return as transmissões retidas, na ordem de registro */
    public Set<Map.Entry<Integer, DeferredTransmission>> entries() {
        return transmissions.entrySet();
    }

    /** @return {@code true} se não há transmissões retidas */
    public boolean empty() {
        return transmissions.isEmpty();
    }

    /** @return quantidade de transmissões retidas */
    public int size() {
        return transmissions.size();
    }
}
