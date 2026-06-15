package demo;

/**
 * Pequeno utilitário para colorir a saída no terminal com códigos ANSI.
 */
public final class Ansi {
    private static final boolean ENABLED = System.getenv("NO_COLOR") == null;

    private static final String CSI = "[";

    public static final String RESET = "0m";
    public static final String BOLD = "1m";
    public static final String DIM = "2m";

    public static final String RED = "31m";
    public static final String GREEN = "32m";
    public static final String YELLOW = "33m";
    public static final String BLUE = "34m";
    public static final String MAGENTA = "35m";
    public static final String CYAN = "36m";
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
