package br.edu.acudes.model;

public enum NivelStatus {
    CRITICO("ALERTA CRÍTICO"),
    ATENCAO("atenção"),
    NORMAL("normal");

    private final String rotulo;

    NivelStatus(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    public static NivelStatus de(double nivelPct) {
        if (nivelPct < 20) return CRITICO;
        if (nivelPct < 40) return ATENCAO;
        return NORMAL;
    }
}
