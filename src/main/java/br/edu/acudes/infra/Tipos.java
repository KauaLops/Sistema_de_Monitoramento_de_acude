package br.edu.acudes.infra;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;

public final class Tipos {
    public static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() {};

    private Tipos() {}
}
