package demo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import CausalMulticast.Envelope;

/**
 * Registro das transmissões retidas pelo cliente.
 *
 * Sempre que o middleware pede para enviar uma mensagem a outro processo, o
 * cliente retém o envelope aqui sob um identificador sequencial em vez de
 * despachá-lo de imediato. Assim o usuário pode escolher quando — e em qual
 * ordem — cada transmissão é enviada (comando /trans), o que permite exercitar
 * a entrega causal embaralhando a rede de propósito.
 */
public final class PendingTransmissions {
    private final Map<Integer, Envelope> envelopes = new LinkedHashMap<>();

    private int nextId = 1;

    public int register(Envelope envelope) {
        int id = nextId++;
        envelopes.put(id, envelope);

        return id;
    }

    public Envelope remove(int id) {
        return envelopes.remove(id);
    }

    public Set<Map.Entry<Integer, Envelope>> entries() {
        return envelopes.entrySet();
    }

    public boolean isEmpty() {
        return envelopes.isEmpty();
    }

    public int size() {
        return envelopes.size();
    }
}
