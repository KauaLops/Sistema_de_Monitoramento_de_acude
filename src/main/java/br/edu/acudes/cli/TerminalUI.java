package br.edu.acudes.cli;

import br.edu.acudes.model.NivelStatus;

public final class TerminalUI {
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String CLEAR_LINE = "\u001B[2K";
    private static final boolean ANSI = ansiDisponivel();

    private TerminalUI() {}

    public static String titulo(String texto) {
        return colorir(texto, BOLD + CYAN);
    }

    public static String destaque(String texto) {
        return colorir(texto, BLUE);
    }

    public static String sucesso(String texto) {
        return colorir(texto, GREEN);
    }

    public static String erro(String texto) {
        return colorir(texto, RED);
    }

    public static String aviso(String texto) {
        return colorir(texto, YELLOW);
    }

    public static String opcao(String texto) {
        return colorir(texto, CYAN);
    }

    public static String status(NivelStatus status) {
        return switch (status) {
            case CRITICO -> colorir(status.rotulo(), RED);
            case ATENCAO -> colorir(status.rotulo(), YELLOW);
            case NORMAL -> colorir(status.rotulo(), GREEN);
        };
    }

    public static void carregar(String mensagem) {
        String[] quadros = {"|", "/", "-", "\\"};
        if (System.console() == null) {
            System.out.println(colorir("[... ] " + mensagem, CYAN));
            pausar(400);
            return;
        }

        for (String quadro : quadros) {
            System.out.print("\r" + CLEAR_LINE + colorir(quadro + " " + mensagem, CYAN));
            System.out.flush();
            pausar(100);
        }
        System.out.print("\r" + CLEAR_LINE + colorir("[OK] " + mensagem, GREEN));
        System.out.println();
    }

    private static String colorir(String texto, String codigo) {
        return ANSI ? codigo + texto + RESET : texto;
    }

    private static boolean ansiDisponivel() {
        if (System.getenv().containsKey("NO_COLOR")) return false;
        String term = System.getenv("TERM");
        return System.console() != null || "1".equals(System.getenv("FORCE_COLOR"))
                || System.getenv().containsKey("WT_SESSION") || System.getenv().containsKey("ANSICON")
                || (term != null && !term.equalsIgnoreCase("dumb"));
    }

    private static void pausar(long milissegundos) {
        try {
            Thread.sleep(milissegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
