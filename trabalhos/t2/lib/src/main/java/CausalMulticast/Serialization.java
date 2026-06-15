package CausalMulticast;

import java.io.*;

/** Conversão entre objetos e bytes para o tráfego pela rede. */
class Serialization {
    /**
     * Serializa um objeto em um vetor de bytes.
     *
     * @param object objeto a serializar
     * @return bytes correspondentes
     * @throws IOException em caso de falha na serialização
     */
    public static byte[] convertToBytes(Object object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(object);
            oos.flush();
            
            return bos.toByteArray();
        }
    }

    /**
     * Reconstrói um objeto a partir de bytes.
     *
     * @param bytes dados serializados
     * @return objeto reconstruído
     * @throws IOException            em caso de falha na leitura
     * @throws ClassNotFoundException se a classe do objeto não for encontrada
     */
    public static Object convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); ObjectInputStream ois = new ObjectInputStream(bis)) {
            return ois.readObject();
        }
    }
}
