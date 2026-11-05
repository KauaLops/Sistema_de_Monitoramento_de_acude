package br.edu.acudes.config;

public record Config(String host, int porta, String banco) {

    public static Config doAmbiente() {
        return new Config(
                System.getenv().getOrDefault("RDB_HOST", "localhost"),
                Integer.parseInt(System.getenv().getOrDefault("RDB_PORT", "28015")),
                System.getenv().getOrDefault("RDB_DB", "acudes"));
    }
}

