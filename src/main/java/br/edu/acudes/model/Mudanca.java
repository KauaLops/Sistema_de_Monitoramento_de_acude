package br.edu.acudes.model;

public record Mudanca(Reservatorio antes, Reservatorio depois) {

    public String tipo() {
        if (antes == null) return "NOVO";
        if (depois == null) return "REMOVIDO";
        return "UPDATE";
    }
}
