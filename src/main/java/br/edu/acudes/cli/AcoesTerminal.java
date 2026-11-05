package br.edu.acudes.cli;

import br.edu.acudes.infra.SchemaInicializador;
import br.edu.acudes.model.Leitura;
import br.edu.acudes.model.Mudanca;
import br.edu.acudes.model.NivelStatus;
import br.edu.acudes.model.Reservatorio;
import br.edu.acudes.service.BackupService;
import br.edu.acudes.service.ReservatorioService;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

public final class AcoesTerminal {
    private final SchemaInicializador schema;
    private final ReservatorioService service;
    private final BackupService backup;

    public AcoesTerminal(SchemaInicializador schema, ReservatorioService service, BackupService backup) {
        this.schema = schema;
        this.service = service;
        this.backup = backup;
    }

    public void executarOpcaoMenu(String opcao, BufferedReader entrada) throws Exception {
        switch (opcao) {
            case "1" -> inicializar();
            case "2" -> cadastrarPeloMenu(entrada);
            case "3" -> listar();
            case "4" -> buscar(lerTexto(entrada, "ID do reservatório"));
            case "5" -> registrarLeituraPeloMenu(entrada);
            case "6" -> removerPeloMenu(entrada);
            case "7" -> porMunicipio(lerTexto(entrada, "Município"));
            case "8" -> criticos(lerNumero(entrada, "Nível máximo inclusivo (0 a 100)"));
            case "9" -> media();
            case "10" -> historico(lerTexto(entrada, "ID do reservatório"));
            case "11" -> observar(entrada);
            case "12" -> simular(lerTexto(entrada, "ID do reservatório para simular"));
            case "13" -> exportar(lerTexto(entrada, "Caminho do arquivo JSON"));
            case "14" -> restaurar(lerTexto(entrada, "Caminho do arquivo JSON"));
            default -> System.out.println(TerminalUI.aviso("Opção inválida. Escolha um número de 0 a 14."));
        }
    }

    public void inicializar() {
        schema.executar(m -> System.out.println(TerminalUI.sucesso(m)));
    }

    public void cadastrar(String nome, String municipio, double capacidade, double nivel) {
        String id = service.cadastrar(nome, municipio, capacidade, nivel);
        System.out.println(TerminalUI.sucesso("Cadastrado, id = " + id));
    }

    public void listar() {
        imprimir(service.listar());
    }

    public void buscar(String id) {
        detalhar(service.buscar(id));
    }

    public void registrarLeitura(String id, double nivel) {
        service.registrarLeitura(id, nivel);
        System.out.println(TerminalUI.sucesso("Leitura registrada e nível atualizado."));
    }

    public void remover(String id) {
        service.remover(id);
        System.out.println(TerminalUI.sucesso("Reservatório e histórico removidos."));
    }

    public void porMunicipio(String municipio) {
        imprimir(service.porMunicipio(municipio));
    }

    public void criticos(double limite) {
        imprimir(service.criticos(limite));
    }

    public void media() {
        var resultado = service.mediaNivel();
        System.out.println(resultado.isPresent()
                ? TerminalUI.sucesso(String.format("Nível médio: %.1f%%", resultado.getAsDouble()))
                : TerminalUI.aviso("Sem dados."));
    }

    public void historico(String id) {
        List<Leitura> leituras = service.historico(id);
        if (leituras.isEmpty()) {
            System.out.println(TerminalUI.aviso("Nenhuma leitura registrada para este reservatório."));
            return;
        }
        leituras.forEach(l -> System.out.println(TerminalUI.opcao(new Date(l.timestamp()).toString())
                + " -> " + l.nivelPct() + "%"));
    }

    public void observar(BufferedReader entrada) throws IOException {
        System.out.println(TerminalUI.aviso(
                "Monitorando reservatórios. Atualize um nível em outro terminal; digite q e Enter para voltar."));
        service.observar((Mudanca m) -> {
            if (m.depois() == null) {
                System.out.println(TerminalUI.erro("[REMOVIDO] " + m.antes().nome()));
                return;
            }
            Reservatorio r = m.depois();
            System.out.printf("[%s] %s (%s): %.1f%% - %s%n",
                    m.tipo(), r.nome(), r.municipio(), r.nivelPct(), TerminalUI.status(r.status()));
        }, () -> {
            try {
                if (!entrada.ready()) return true;
                String comando = entrada.readLine();
                if (comando == null || comando.trim().equalsIgnoreCase("q")) return false;
                System.out.println(TerminalUI.aviso("Para voltar ao menu, digite q e pressione Enter."));
                return true;
            } catch (IOException e) {
                System.err.println(TerminalUI.erro("Erro ao ler a entrada do terminal: " + e.getMessage()));
                return false;
            }
        });
        System.out.println(TerminalUI.sucesso("Monitoramento encerrado."));
    }

    public void simular(String id) throws InterruptedException {
        Reservatorio selecionado = service.buscar(id);
        if (selecionado.nivelPct() <= 0) {
            System.out.println(TerminalUI.aviso("O reservatório já está em 0%; não há queda para simular."));
            return;
        }
        int rodadas = 10;
        System.out.println(TerminalUI.aviso(
                "Simulação temporária de " + selecionado.nome() + ". O nível original será restaurado;"
                        + " o histórico não será alterado."));
        int rodadasExecutadas = service.simularQueda(id, rodadas, 2000, (reservatorio, novoNivel) ->
                        System.out.printf("%s: %.1f%% -> %.1f%% [%s]%n",
                                reservatorio.nome(), reservatorio.nivelPct(), novoNivel,
                                TerminalUI.status(NivelStatus.de(novoNivel))),
                rodada -> System.out.println(TerminalUI.opcao("Rodada " + rodada + "/" + rodadas + " concluída.")));
        if (rodadasExecutadas < rodadas) {
            System.out.println(TerminalUI.aviso(
                    "Reservatório chegou a 0%; simulação encerrada na rodada " + rodadasExecutadas + "."));
        } else {
            System.out.println(TerminalUI.sucesso("Simulação concluída após " + rodadas + " rodadas."));
        }
        System.out.println(TerminalUI.sucesso(
                "Nível original de " + selecionado.nivelPct() + "% restaurado. Retornando ao menu."));
    }

    public void exportar(String arquivo) throws Exception {
        System.out.println(TerminalUI.sucesso(
                backup.exportar(Path.of(arquivo)) + " documentos exportados."));
    }

    public void restaurar(String arquivo) throws Exception {
        System.out.println(TerminalUI.sucesso(
                backup.importar(Path.of(arquivo)) + " documentos restaurados."));
    }

    private void cadastrarPeloMenu(BufferedReader entrada) throws IOException {
        String nome = lerTexto(entrada, "Nome do reservatório");
        String municipio = lerTexto(entrada, "Município");
        double capacidade = lerNumero(entrada, "Capacidade em hm³");
        double nivel = lerNumero(entrada, "Nível atual em %");
        cadastrar(nome, municipio, capacidade, nivel);
    }

    private void registrarLeituraPeloMenu(BufferedReader entrada) throws IOException {
        String id = lerTexto(entrada, "ID do reservatório");
        double nivel = lerNumero(entrada, "Novo nível em %");
        registrarLeitura(id, nivel);
    }

    private void removerPeloMenu(BufferedReader entrada) throws IOException {
        String id = lerTexto(entrada, "ID do reservatório a remover");
        String confirmacao = lerTexto(entrada, "Confirma a remoção do reservatório e do histórico relacionado? (s/N)");
        if (!confirmacao.equalsIgnoreCase("s")) {
            System.out.println(TerminalUI.aviso("Remoção cancelada."));
            return;
        }
        remover(id);
    }

    private String lerTexto(BufferedReader entrada, String rotulo) throws IOException {
        System.out.print(TerminalUI.destaque(rotulo + ": "));
        String texto = entrada.readLine();
        if (texto == null) throw new IllegalStateException("A entrada do terminal foi encerrada.");
        return texto.trim();
    }

    private double lerNumero(BufferedReader entrada, String rotulo) throws IOException {
        return Double.parseDouble(lerTexto(entrada, rotulo).replace(',', '.'));
    }

    private void imprimir(List<Reservatorio> lista) {
        if (lista.isEmpty()) {
            System.out.println(TerminalUI.aviso("Nenhum reservatório encontrado."));
            return;
        }
        lista.forEach(r -> System.out.printf("%-38s %-22s %-18s %6.1f%%  [%s]%n",
                r.id(), r.nome(), r.municipio(), r.nivelPct(), TerminalUI.status(r.status())));
    }

    private void detalhar(Reservatorio r) {
        System.out.printf("Id: %s%nNome: %s%nMunicípio: %s%nCapacidade: %.1f hm³%nNível: %.1f%% (%s)%n",
                r.id(), r.nome(), r.municipio(), r.capacidadeHm3(), r.nivelPct(), TerminalUI.status(r.status()));
    }
}
