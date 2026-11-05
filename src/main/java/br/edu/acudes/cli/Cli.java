package br.edu.acudes.cli;

import br.edu.acudes.infra.SchemaInicializador;
import br.edu.acudes.service.BackupService;
import br.edu.acudes.service.RegraDeNegocioException;
import br.edu.acudes.service.ReservatorioService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class Cli {
    private static final Set<String> COMANDOS = Set.of(
            "init", "add", "list", "get", "leitura", "delete", "municipio", "criticos",
            "media", "historico", "watch", "simulate", "backup", "restore", "menu", "help", "--help", "-h");

    private final AcoesTerminal acoes;
    private final MenuTerminal menu;

    public Cli(SchemaInicializador schema, ReservatorioService service, BackupService backup) {
        this.acoes = new AcoesTerminal(schema, service, backup);
        this.menu = new MenuTerminal(acoes);
    }

    public void executar(String[] argumentos) throws Exception {
        if (argumentos.length == 0 || argumentos[0].equals("menu")) {
            menu.executar();
            return;
        }

        try {
            switch (argumentos[0]) {
                case "help", "--help", "-h" -> uso();
                case "init" -> acoes.inicializar();
                case "add" -> acoes.cadastrar(argumentos[1], argumentos[2],
                        Double.parseDouble(argumentos[3]), Double.parseDouble(argumentos[4]));
                case "list" -> acoes.listar();
                case "get" -> acoes.buscar(argumentos[1]);
                case "leitura" -> acoes.registrarLeitura(argumentos[1], Double.parseDouble(argumentos[2]));
                case "delete" -> acoes.remover(argumentos[1]);
                case "municipio" -> acoes.porMunicipio(argumentos[1]);
                case "criticos" -> acoes.criticos(Double.parseDouble(argumentos[1]));
                case "media" -> acoes.media();
                case "historico" -> acoes.historico(argumentos[1]);
                case "watch" -> acoes.observar(
                        new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)));
                case "simulate" -> acoes.simular(argumentos[1]);
                case "backup" -> acoes.exportar(argumentos[1]);
                case "restore" -> acoes.restaurar(argumentos[1]);
                default -> uso();
            }
        } catch (RegraDeNegocioException e) {
            System.err.println(TerminalUI.erro("Erro: " + e.getMessage()));
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println(TerminalUI.erro("Argumentos insuficientes."));
            uso();
        } catch (NumberFormatException e) {
            System.err.println(TerminalUI.erro("Valor numérico inválido: " + e.getMessage()));
        }
    }

    public static void uso() {
        System.out.println("""
                Uso: mvn exec:java -Dexec.args="<comando> [argumentos]"
                     mvn exec:java                         (abre o menu interativo)

                Comandos diretos:
                  init
                  add <nome> <municipio> <capacidade_hm3> <nivel_pct>
                  list | get <id> | delete <id>
                  leitura <id> <nivel_pct>     (atualiza reservatório e grava histórico)
                  municipio <nome> | criticos <limite> | media | historico <id>
                  watch                        (changefeed em tempo real)
                  simulate <id>                (simula queda de nível para um reservatório)
                  backup <arquivo.json> | restore <arquivo.json> | menu
                  help""");
    }

    public static boolean comandoConhecido(String comando) {
        return COMANDOS.contains(comando);
    }
}
