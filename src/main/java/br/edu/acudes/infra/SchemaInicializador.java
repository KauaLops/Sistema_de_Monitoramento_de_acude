package br.edu.acudes.infra;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.exc.ReqlError;

import java.util.function.Consumer;
import java.util.Locale;

public class SchemaInicializador {
    private static final RethinkDB r = RethinkDB.r;
    private final Conexao conexao;

    public SchemaInicializador(Conexao conexao) {
        this.conexao = conexao;
    }

    public void executar(Consumer<String> log) {
        var conn = conexao.get();
        String db = conexao.banco();

        tentar(() -> r.dbCreate(db).run(conn).first(), "Banco " + db, log);
        for (String tabela : new String[]{"reservatorios", "leituras"}) {
            tentar(() -> r.db(db).tableCreate(tabela).run(conn).first(), "Tabela " + tabela, log);
        }
        tentar(() -> conexao.tabela("leituras").indexCreate("reservatorio_id").run(conn).first(),
                "Índice reservatorio_id", log);
        conexao.tabela("leituras").indexWait("reservatorio_id").run(conn).toList();
    }

    private void tentar(Runnable acao, String descricao, Consumer<String> log) {
        try {
            acao.run();
            log.accept(descricao + ": criado");
        } catch (ReqlError e) {
            String mensagem = e.getMessage();
            if (mensagem == null || !mensagem.toLowerCase(Locale.ROOT).contains("already exists")) {
                throw e;
            }
            log.accept(descricao + ": já existe");
        }
    }
}
