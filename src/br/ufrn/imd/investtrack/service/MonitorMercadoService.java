package br.ufrn.imd.investtrack.service;

import br.ufrn.imd.investtrack.exception.CotacaoIndisponivelException;
import br.ufrn.imd.investtrack.interfaces.ProvedorCotacao;
import br.ufrn.imd.investtrack.model.AtivoFinanceiro;
import br.ufrn.imd.investtrack.repository.CarteiraRepository;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/*
 A cada 15 segundos, busca a cotacao de cada ticker da carteira e atualiza o ativo com atualizarCotacao.
 Roda numa thread separada (ScheduledExecutorService de java.util.concurrent) para a janela nao travar
 enquanto espera a resposta da internet.
 O provedor eh uma interface (Strategy): pode ser a API real ou o simulado, e da para trocar com o programa rodando.
*/
public class MonitorMercadoService {
    private static final int INTERVALO_SEGUNDOS = 15;

    private final CarteiraRepository repositorio;
    private volatile ProvedorCotacao provedor; // volatile: a troca feita pela tela fica visivel para a thread do monitor
    private ScheduledExecutorService agendador;

    public MonitorMercadoService(ProvedorCotacao provedor, CarteiraRepository repositorio) {
        this.provedor = Objects.requireNonNull(provedor, "provedor nao pode ser nulo");
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio nao pode ser nulo");
    }

    // Troca a fonte das cotacoes; vale a partir do proximo ciclo
    public void setProvedor(ProvedorCotacao provedor) {
        this.provedor = Objects.requireNonNull(provedor, "provedor nao pode ser nulo");
    }

    // Comeca a monitorar: o primeiro ciclo roda na hora e os proximos a cada 15 segundos
    public synchronized void iniciar() {
        if (agendador != null) {
            return; // ja esta rodando
        }
        agendador = Executors.newSingleThreadScheduledExecutor(tarefa -> {
            Thread thread = new Thread(tarefa, "monitor-mercado");
            thread.setDaemon(true); // nao impede o programa de fechar
            return thread;
        });
        agendador.scheduleAtFixedRate(this::executarCiclo, 0, INTERVALO_SEGUNDOS, TimeUnit.SECONDS);
    }

    // Para de monitorar e interrompe uma consulta que esteja em andamento
    public synchronized void parar() {
        if (agendador == null) {
            return;
        }
        agendador.shutdownNow();
        agendador = null;
    }

    public synchronized boolean isRodando() {
        return agendador != null;
    }

    /*
      Um ciclo de atualizacao. Eh publico para os testes chamarem na hora, sem esperar 15 segundos.
      Se a cotacao de um ticker falhar, o ativo fica com a ultima cotacao e o ciclo segue para o proximo.
    */
    public void executarCiclo() {
        for (String ticker : repositorio.listarTickersMonitorados()) {
            try {
                double cotacao = provedor.buscarCotacaoAtual(ticker);
                AtivoFinanceiro ativo = repositorio.buscar(ticker);
                if (ativo != null) { // pode ter sido vendido durante a consulta
                    ativo.atualizarCotacao(cotacao);
                }
            } catch (CotacaoIndisponivelException | RuntimeException erro) {
                // RuntimeException tambem: se escapar uma, o ScheduledExecutorService cancela os proximos ciclos sem avisar
                System.err.println("Cotacao de " + ticker + " nao atualizada: " + erro.getMessage());
            }
        }
    }
}
