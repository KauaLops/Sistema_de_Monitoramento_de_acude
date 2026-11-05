package br.edu.acudes.cli;

import br.edu.acudes.service.RegraDeNegocioException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class MenuTerminal {
    private final AcoesTerminal acoes;

    public MenuTerminal(AcoesTerminal acoes) {
        this.acoes = acoes;
    }

    public void executar() {
        BufferedReader entrada = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        TerminalUI.carregar("Iniciando aplicação");

        while (true) {
            imprimirMenu();
            System.out.print(TerminalUI.destaque("Selecione uma opção: "));
            String opcao;
            try {
                opcao = entrada.readLine();
            } catch (IOException e) {
                System.err.println(TerminalUI.erro("Erro ao ler a opção: " + e.getMessage()));
                return;
            }
            if (opcao == null) return;
            opcao = opcao.trim();
            if (opcao.equals("0")) {
                System.out.println(TerminalUI.sucesso("Até logo."));
                return;
            }

            try {
                TerminalUI.carregar("Carregando opção " + opcao);
                acoes.executarOpcaoMenu(opcao, entrada);
            } catch (RegraDeNegocioException e) {
                System.err.println(TerminalUI.erro("Erro: " + e.getMessage()));
            } catch (NumberFormatException e) {
                System.err.println(TerminalUI.erro("Valor numérico inválido."));
            } catch (Exception e) {
                System.err.println(TerminalUI.erro(
                        "Não foi possível concluir a operação: " + e.getMessage()));
            } finally {
                TerminalUI.carregar("Atualizando menu");
            }
        }
    }

    private void imprimirMenu() {
        System.out.println("\n" + TerminalUI.titulo("=============================================="));
        System.out.println(TerminalUI.titulo(" MONITORAMENTO DE AÇUDES DO SEMIÁRIDO"));
        System.out.println(TerminalUI.titulo("=============================================="));
        System.out.println(TerminalUI.opcao(" 1.") + " Inicializar banco de dados");
        System.out.println(TerminalUI.opcao(" 2.") + " Cadastrar reservatório");
        System.out.println(TerminalUI.opcao(" 3.") + " Listar reservatórios");
        System.out.println(TerminalUI.opcao(" 4.") + " Consultar reservatório por ID");
        System.out.println(TerminalUI.opcao(" 5.") + " Registrar leitura / atualizar nível");
        System.out.println(TerminalUI.opcao(" 6.") + " Remover reservatório");
        System.out.println(TerminalUI.opcao(" 7.") + " Consultar por município");
        System.out.println(TerminalUI.opcao(" 8.") + " Listar níveis críticos");
        System.out.println(TerminalUI.opcao(" 9.") + " Calcular nível médio");
        System.out.println(TerminalUI.opcao("10.") + " Consultar histórico de leituras");
        System.out.println(TerminalUI.opcao("11.") + " Acompanhar mudanças em tempo real");
        System.out.println(TerminalUI.opcao("12.") + " Simular queda de nível por ID");
        System.out.println(TerminalUI.opcao("13.") + " Exportar backup JSON");
        System.out.println(TerminalUI.opcao("14.") + " Restaurar backup JSON");
        System.out.println(TerminalUI.opcao(" 0.") + " Sair");
    }
}
