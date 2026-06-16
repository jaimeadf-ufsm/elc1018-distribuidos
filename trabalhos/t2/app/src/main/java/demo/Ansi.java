package demo;

/**
 * Pequeno utilitário para colorir a saída no terminal com códigos ANSI.
 */
public final class Ansi {
    /** Indica se a coloração está ativa. */
    private static final boolean ENABLED = System.getenv("NO_COLOR") == null;

    /** Prefixo das sequências de escape ANSI. */
    private static final String CSI ="[";

    /** Remove toda a formatação. */
    public static final String RESET = "0m";

    /** Negrito. */
    public static final String BOLD = "1m";

    /** Esmaecido. */
    public static final String DIM = "2m";

    /** Vermelho. */
    public static final String RED = "31m";

    /** Verde. */
    public static final String GREEN = "32m";

    /** Amarelo. */
    public static final String YELLOW = "33m";

    /** Azul. */
    public static final String BLUE = "34m";

    /** Magenta. */
    public static final String MAGENTA = "35m";

    /** Ciano. */
    public static final String CYAN = "36m";

    /** Cinza. */
    public static final String GRAY = "90m";

    private Ansi() {}

    /**
     * Aplica estilos ANSI ao texto.
     *
     * @param text   texto a colorir
     * @param styles códigos de estilo a aplicar
     * @return texto formatado
     */
    public static String paint(String text, String... styles) {
        if (!ENABLED || styles.length == 0) {
            return text;
        }

        StringBuilder builder = new StringBuilder();

        for (String style : styles) {
            builder.append(CSI).append(style);
        }

        builder.append(text).append(CSI).append(RESET);

        return builder.toString();
    }
}
