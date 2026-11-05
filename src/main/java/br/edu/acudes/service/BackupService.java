package br.edu.acudes.service;

import br.edu.acudes.repository.LeituraRepository;
import br.edu.acudes.repository.ReservatorioRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BackupService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TIPO = new TypeToken<Map<String, List<Map<String, Object>>>>() {}.getType();

    private final ReservatorioRepository reservatorios;
    private final LeituraRepository leituras;

    public BackupService(ReservatorioRepository reservatorios, LeituraRepository leituras) {
        this.reservatorios = reservatorios;
        this.leituras = leituras;
    }

    public int exportar(Path arquivo) throws IOException {
        Map<String, List<Map<String, Object>>> dump = new LinkedHashMap<>();
        dump.put("reservatorios", reservatorios.listarDocumentos());
        dump.put("leituras", leituras.listarDocumentos());
        try (Writer w = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8)) {
            GSON.toJson(dump, w);
        }
        return dump.values().stream().mapToInt(List::size).sum();
    }

    public int importar(Path arquivo) throws IOException {
        Map<String, List<Map<String, Object>>> dump;
        try (Reader rd = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
            dump = GSON.fromJson(rd, TIPO);
        }
        if (dump == null) {
            throw new IOException("Arquivo de backup vazio ou inválido.");
        }
        var res = dump.get("reservatorios");
        var lei = dump.get("leituras");
        if (res == null) res = List.of();
        if (lei == null) lei = List.of();
        reservatorios.inserirDocumentos(res);
        leituras.inserirDocumentos(lei);
        return res.size() + lei.size();
    }
}
