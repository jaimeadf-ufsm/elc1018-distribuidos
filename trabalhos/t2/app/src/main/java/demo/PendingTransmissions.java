package demo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import CausalMulticast.DeferredTransmission;

/**
 * Registro das transmissões retidas pelo cliente.
 *
 * Sempre que o middleware pede para enviar uma mensagem a outro processo, o
 * cliente retém o transmission aqui sob um identificador sequencial em vez de
 * despachá-lo de imediato. Assim o usuário pode escolher quando — e em qual
 * ordem — cada transmissão é enviada (comando /trans), o que permite exercitar
 * a entrega causal embaralhando a rede de propósito.
 */
public final class PendingTransmissions {
    private final Map<Integer, DeferredTransmission> transmissions = new LinkedHashMap<>();

    private int nextId = 1;

    public int register(DeferredTransmission transmission) {
        int id = nextId++;
        transmissions.put(id, transmission);

        return id;
    }

    public DeferredTransmission remove(int id) {
        return transmissions.remove(id);
    }

    public Set<Map.Entry<Integer, DeferredTransmission>> entries() {
        return transmissions.entrySet();
    }

    public boolean isEmpty() {
        return transmissions.isEmpty();
    }

    public int size() {
        return transmissions.size();
    }
}
