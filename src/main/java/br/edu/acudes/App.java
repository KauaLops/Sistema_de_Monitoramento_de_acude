package br.edu.acudes;

import br.edu.acudes.cli.Cli;
import br.edu.acudes.config.Config;
import br.edu.acudes.infra.Conexao;
import br.edu.acudes.infra.SchemaInicializador;
import br.edu.acudes.repository.LeituraRepository;
import br.edu.acudes.repository.ReservatorioRepository;
import br.edu.acudes.service.BackupService;
import br.edu.acudes.service.ReservatorioService;

public class App {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && !Cli.comandoConhecido(args[0])) {
            Cli.uso();
            return;
        }
        if (args.length > 0 && (args[0].equals("help") || args[0].equals("--help") || args[0].equals("-h"))) {
            Cli.uso();
            return;
        }
        Config config = Config.doAmbiente();
        Conexao conexao;
        try {
            conexao = new Conexao(config);
        } catch (RuntimeException e) {
            System.err.printf("Não foi possível conectar ao RethinkDB em %s:%d.%n",
                    config.host(), config.porta());
            System.err.println("Inicie o banco com 'docker compose up -d' e tente novamente.");
            System.err.println("Detalhe: " + e.getMessage());
            return;
        }

        try (conexao) {
            var reservatorios = new ReservatorioRepository(conexao);
            var leituras = new LeituraRepository(conexao);
            var service = new ReservatorioService(reservatorios, leituras);
            var backup = new BackupService(reservatorios, leituras);
            var schema = new SchemaInicializador(conexao);

            new Cli(schema, service, backup).executar(args);
        }
    }
}
