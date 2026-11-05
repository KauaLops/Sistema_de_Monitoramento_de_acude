package br.edu.acudes.model;

import java.util.Map;

public record Leitura(String reservatorioId, double nivelPct, long timestamp) {

    public static Leitura de(Map<String, Object> doc) {
        return new Leitura(
                (String) doc.get("reservatorio_id"),
                ((Number) doc.get("nivel_pct")).doubleValue(),
                ((Number) doc.get("ts")).longValue());
    }
}
