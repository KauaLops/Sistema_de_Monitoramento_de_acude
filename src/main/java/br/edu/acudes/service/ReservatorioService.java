package br.edu.acudes.service;

import br.edu.acudes.model.Leitura;
import br.edu.acudes.model.Mudanca;
import br.edu.acudes.model.Reservatorio;
import br.edu.acudes.repository.LeituraRepository;
import br.edu.acudes.repository.ReservatorioRepository;

import java.util.List;
import java.util.OptionalDouble;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class ReservatorioService {
    private final ReservatorioRepository reservatorios;
    private final LeituraRepository leituras;

    public ReservatorioService(ReservatorioRepository reservatorios, LeituraRepository leituras) {
        this.reservatorios = reservatorios;
        this.leituras = leituras;
    }

    public String cadastrar(String nome, String municipio, double capacidadeHm3, double nivelPct) {
        if (nome == null || nome.isBlank()) throw new RegraDeNegocioException("Nome é obrigatório.");
        if (municipio == null || municipio.isBlank()) throw new RegraDeNegocioException("Município é obrigatório.");
        if (!Double.isFinite(capacidadeHm3) || capacidadeHm3 <= 0) {
            throw new RegraDeNegocioException("Capacidade deve ser um número maior que zero.");
        }
        validarNivel(nivelPct);
        return reservatorios.criar(nome.trim(), municipio.trim(), capacidadeHm3, nivelPct);
    }

    public Reservatorio buscar(String id) {
        return reservatorios.buscarPorId(id)
                .orElseThrow(() -> new RegraDeNegocioException("Reservatório não encontrado: " + id));
    }

    public List<Reservatorio> listar() {
        return reservatorios.listar();
    }

    public List<Reservatorio> porMunicipio(String municipio) {
        return reservatorios.porMunicipio(municipio);
    }

    public List<Reservatorio> criticos(double limite) {
        if (!Double.isFinite(limite) || limite < 0 || limite > 100) {
            throw new RegraDeNegocioException("Limite deve estar entre 0 e 100.");
        }
        return reservatorios.noLimiteOuAbaixo(limite);
    }

    public OptionalDouble mediaNivel() {
        return reservatorios.mediaNivel();
    }

    public List<Leitura> historico(String id) {
        buscar(id);
        return leituras.historico(id);
    }

    public void registrarLeitura(String id, double nivelPct) {
        validarNivel(nivelPct);
        if (!reservatorios.atualizarNivel(id, nivelPct)) {
            throw new RegraDeNegocioException("Reservatório não encontrado: " + id);
        }
        leituras.registrar(id, nivelPct);
    }

    public void remover(String id) {
        if (!reservatorios.remover(id)) {
            throw new RegraDeNegocioException("Reservatório não encontrado: " + id);
        }
        leituras.removerPorReservatorio(id);
    }

    public void observar(Consumer<Mudanca> ouvinte) {
        observar(ouvinte, () -> true);
    }

    public void observar(Consumer<Mudanca> ouvinte, BooleanSupplier continuar) {
        reservatorios.observar(ouvinte, continuar);
    }

    public int simularQueda(String reservatorioId, int rodadas, long intervaloMs,
                            BiConsumer<Reservatorio, Double> aoAtualizarNivel,
                            IntConsumer aoConcluirRodada) throws InterruptedException {
        Reservatorio inicial = buscar(reservatorioId);
        int rodadasConcluidas = 0;
        Random rnd = new Random();
        try {
            for (int i = 0; i < rodadas; i++) {
                Reservatorio reservatorio = buscar(reservatorioId);
                double novo = Math.max(0, reservatorio.nivelPct() - rnd.nextDouble() * 6);
                double nivelFinal = Math.round(novo * 10) / 10.0;
                if (!reservatorios.atualizarNivelSimulado(reservatorioId, nivelFinal)) {
                    throw new RegraDeNegocioException("Reservatório não encontrado: " + reservatorioId);
                }
                aoAtualizarNivel.accept(reservatorio, nivelFinal);
                rodadasConcluidas = i + 1;
                aoConcluirRodada.accept(rodadasConcluidas);
                if (nivelFinal <= 0) break;
                if (i + 1 < rodadas) Thread.sleep(intervaloMs);
            }
            return rodadasConcluidas;
        } finally {
            reservatorios.atualizarNivelSimulado(reservatorioId, inicial.nivelPct());
        }
    }

    private void validarNivel(double nivelPct) {
        if (!Double.isFinite(nivelPct) || nivelPct < 0 || nivelPct > 100) {
            throw new RegraDeNegocioException("Nível deve estar entre 0 e 100.");
        }
    }
}
