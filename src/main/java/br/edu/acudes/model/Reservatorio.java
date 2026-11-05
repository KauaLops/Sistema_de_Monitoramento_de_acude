package br.edu.acudes.model;

import java.util.Map;

public record Reservatorio(String id, String nome, String municipio,
                           double capacidadeHm3, double nivelPct) {

    public NivelStatus status() {
        return NivelStatus.de(nivelPct);
    }

    public static Reservatorio de(Map<String, Object> doc) {
        return new Reservatorio(
                (String) doc.get("id"),
                (String) doc.get("nome"),
                (String) doc.get("municipio"),
                numero(doc.get("capacidade_hm3")),
                numero(doc.get("nivel_pct")));
    }

    static double numero(Object o) {
        return o == null ? 0.0 : ((Number) o).doubleValue();
    }
}
