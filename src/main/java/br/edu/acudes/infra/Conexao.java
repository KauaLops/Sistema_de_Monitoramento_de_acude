package br.edu.acudes.infra;

import br.edu.acudes.config.Config;
import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.ast.Table;
import com.rethinkdb.net.Connection;

public class Conexao implements AutoCloseable {
    private final Connection conn;
    private final String banco;

    public Conexao(Config config) {
        this.conn = RethinkDB.r.connection()
                .hostname(config.host())
                .port(config.porta())
                .connect();
        this.banco = config.banco();
    }

    public Connection get() {
        return conn;
    }

    public String banco() {
        return banco;
    }

    public Table tabela(String nome) {
        return RethinkDB.r.db(banco).table(nome);
    }

    @Override
    public void close() {
        conn.close();
    }
}
