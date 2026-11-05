package br.edu.acudes.repository;

import br.edu.acudes.infra.Conexao;
import br.edu.acudes.infra.Tipos;
import br.edu.acudes.model.Leitura;
import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.ast.Table;
import com.rethinkdb.net.Result;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class LeituraRepository {
    private static final RethinkDB r = RethinkDB.r;
    private static final String INDICE = "reservatorio_id";
    private final Conexao conexao;

    public LeituraRepository(Conexao conexao) {
        this.conexao = conexao;
    }

    private Table tabela() {
        return conexao.tabela("leituras");
    }

    public void registrar(String reservatorioId, double nivelPct) {
        tabela().insert(r.hashMap("reservatorio_id", reservatorioId)
                .with("nivel_pct", nivelPct)
                .with("ts", System.currentTimeMillis()))
                .run(conexao.get()).first();
    }

    public List<Leitura> historico(String reservatorioId) {
        return coletar(() -> tabela().getAll(reservatorioId)
                .optArg("index", INDICE)
                .run(conexao.get(), Tipos.MAPA))
                .stream()
                .map(Leitura::de)
                .sorted(Comparator.comparingLong(Leitura::timestamp))
                .toList();
    }

    public void removerPorReservatorio(String reservatorioId) {
        tabela().getAll(reservatorioId).optArg("index", INDICE).delete()
                .run(conexao.get()).first();
    }

    public List<Map<String, Object>> listarDocumentos() {
        return coletar(() -> tabela().run(conexao.get(), Tipos.MAPA));
    }

    public void inserirDocumentos(List<Map<String, Object>> docs) {
        if (docs.isEmpty()) return;
        tabela().insert(docs).optArg("conflict", "replace").run(conexao.get()).first();
    }

    private List<Map<String, Object>> coletar(Supplier<Result<Map<String, Object>>> consulta) {
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Result<Map<String, Object>> resultado = consulta.get()) {
            for (Map<String, Object> doc : resultado) lista.add(doc);
        }
        return lista;
    }
}
