package br.edu.acudes.repository;

import br.edu.acudes.infra.Conexao;
import br.edu.acudes.infra.Tipos;
import br.edu.acudes.model.Mudanca;
import br.edu.acudes.model.Reservatorio;
import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.ast.Table;
import com.rethinkdb.gen.exc.ReqlError;
import com.rethinkdb.net.Result;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ReservatorioRepository {
    private static final RethinkDB r = RethinkDB.r;
    private final Conexao conexao;

    public ReservatorioRepository(Conexao conexao) {
        this.conexao = conexao;
    }

    private Table tabela() {
        return conexao.tabela("reservatorios");
    }

    public String criar(String nome, String municipio, double capacidadeHm3, double nivelPct) {
        Map<String, Object> res = tabela().insert(
                r.hashMap("nome", nome)
                        .with("municipio", municipio)
                        .with("capacidade_hm3", capacidadeHm3)
                        .with("nivel_pct", nivelPct)
                        .with("criado_em", System.currentTimeMillis()))
                .run(conexao.get(), Tipos.MAPA).first();
        return (String) ((List<?>) res.get("generated_keys")).get(0);
    }

    public Optional<Reservatorio> buscarPorId(String id) {
        return buscarDocumento(id).map(Reservatorio::de);
    }

    public Optional<Map<String, Object>> buscarDocumento(String id) {
        Map<String, Object> doc = tabela().get(id).run(conexao.get(), Tipos.MAPA).first();
        return Optional.ofNullable(doc);
    }

    public List<Reservatorio> listar() {
        return converter(() -> tabela().run(conexao.get(), Tipos.MAPA));
    }

    public List<Reservatorio> porMunicipio(String municipio) {
        return converter(() -> tabela()
                .filter(r.hashMap("municipio", municipio))
                .run(conexao.get(), Tipos.MAPA));
    }

    public List<Reservatorio> noLimiteOuAbaixo(double limite) {
        return converter(() -> tabela()
                .filter(row -> row.g("nivel_pct").le(limite))
                .run(conexao.get(), Tipos.MAPA));
    }

    public OptionalDouble mediaNivel() {
        Object media = tabela().avg("nivel_pct").default_((Object) null).run(conexao.get()).first();
        return media instanceof Number numero
                ? OptionalDouble.of(numero.doubleValue())
                : OptionalDouble.empty();
    }

    public boolean atualizarNivel(String id, double nivelPct) {
        Map<String, Object> res = tabela().get(id)
                .update(r.hashMap("nivel_pct", nivelPct)
                        .with("atualizado_em", System.currentTimeMillis()))
                .run(conexao.get(), Tipos.MAPA).first();
        return ((Number) res.get("skipped")).longValue() == 0;
    }

    public boolean atualizarNivelSimulado(String id, double nivelPct) {
        Map<String, Object> res = tabela().get(id)
                .update(r.hashMap("nivel_pct", nivelPct))
                .run(conexao.get(), Tipos.MAPA).first();
        return ((Number) res.get("skipped")).longValue() == 0;
    }

    public boolean remover(String id) {
        Map<String, Object> res = tabela().get(id).delete()
                .run(conexao.get(), Tipos.MAPA).first();
        return ((Number) res.get("deleted")).longValue() > 0;
    }

    @SuppressWarnings("unchecked")
    public void observar(Consumer<Mudanca> ouvinte, BooleanSupplier continuar) {
        try (Result<Map<String, Object>> feed = tabela().changes().run(conexao.get(), Tipos.MAPA)) {
            while (continuar.getAsBoolean()) {
                try {
                    Map<String, Object> mudanca = feed.next(250, TimeUnit.MILLISECONDS);
                    var antes = (Map<String, Object>) mudanca.get("old_val");
                    var depois = (Map<String, Object>) mudanca.get("new_val");
                    ouvinte.accept(new Mudanca(
                            antes == null ? null : Reservatorio.de(antes),
                            depois == null ? null : Reservatorio.de(depois)));
                } catch (TimeoutException ignorado) {
                }
            }
        }
    }

    public List<Map<String, Object>> listarDocumentos() {
        return coletar(() -> tabela().run(conexao.get(), Tipos.MAPA));
    }

    public void inserirDocumentos(List<Map<String, Object>> docs) {
        if (docs.isEmpty()) return;
        tabela().insert(docs).optArg("conflict", "replace").run(conexao.get()).first();
    }

    private List<Reservatorio> converter(Supplier<Result<Map<String, Object>>> consulta) {
        return coletar(consulta).stream().map(Reservatorio::de).toList();
    }

    private List<Map<String, Object>> coletar(Supplier<Result<Map<String, Object>>> consulta) {
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Result<Map<String, Object>> resultado = consulta.get()) {
            for (Map<String, Object> doc : resultado) lista.add(doc);
        }
        return lista;
    }
}
