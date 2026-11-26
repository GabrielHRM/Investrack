package br.ufrn.imd.investtrack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import br.ufrn.imd.investtrack.exception.CotacaoIndisponivelException;
import br.ufrn.imd.investtrack.interfaces.ProvedorCotacao;
import br.ufrn.imd.investtrack.model.Acao;
import br.ufrn.imd.investtrack.model.AtivoFinanceiro;
import br.ufrn.imd.investtrack.model.Criptomoeda;
import br.ufrn.imd.investtrack.model.RendaFixa;
import br.ufrn.imd.investtrack.repository.CarteiraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*
 Os provedores destes testes sao lambdas: ProvedorCotacao tem um metodo so,
 entao da para criar um provedor falso com precos fixos, sem internet.
*/
class MonitorMercadoServiceTest {
    private CarteiraRepository repositorio;

    // O Singleton eh compartilhado entre os testes, entao a carteira comeca vazia em cada um
    @BeforeEach
    void prepararCarteiraVazia() {
        repositorio = CarteiraRepository.getInstancia();
        for (AtivoFinanceiro ativo : repositorio.listarTodos()) {
            repositorio.remover(ativo.getTicker());
        }
    }

    @Test
    void cicloAtualizaACotacaoDeCadaAtivoDaCarteira() {
        repositorio.salvar(new Acao("PETR4", "Petrobras", 10.0, 30.0));
        repositorio.salvar(new Criptomoeda("BTCBRL", "Bitcoin", 0.01, 300_000.0));
        ProvedorCotacao provedor = ticker -> ticker.equals("PETR4") ? 35.0 : 320_000.0;
        MonitorMercadoService monitor = new MonitorMercadoService(provedor, repositorio);

        monitor.executarCiclo();

        assertEquals(35.0, repositorio.buscar("PETR4").getCotacaoAtual(), 1.0e-9);
        assertEquals(320_000.0, repositorio.buscar("BTCBRL").getCotacaoAtual(), 1.0e-9);
    }

    @Test
    void rendaFixaNaoEhConsultada() {
        repositorio.salvar(new Acao("PETR4", "Petrobras", 10.0, 30.0));
        repositorio.salvar(new RendaFixa("CDB Banco X", 1_000.0, 0.12, LocalDate.now().plusYears(1)));
        List<String> consultados = new CopyOnWriteArrayList<>();
        ProvedorCotacao provedor = ticker -> {
            consultados.add(ticker);
            return 31.0;
        };

        new MonitorMercadoService(provedor, repositorio).executarCiclo();

        assertEquals(List.of("PETR4"), consultados);
    }

    @Test
    void falhaEmUmTickerNaoImpedeOsOutrosEMantemAUltimaCotacao() {
        repositorio.salvar(new Acao("PETR4", "Petrobras", 10.0, 30.0));
        repositorio.salvar(new Acao("VALE3", "Vale", 5.0, 60.0));
        ProvedorCotacao provedor = ticker -> {
            if (ticker.equals("PETR4")) {
                throw new CotacaoIndisponivelException("sem internet");
            }
            return 65.0;
        };

        new MonitorMercadoService(provedor, repositorio).executarCiclo();

        assertEquals(30.0, repositorio.buscar("PETR4").getCotacaoAtual(), 1.0e-9);
        assertEquals(65.0, repositorio.buscar("VALE3").getCotacaoAtual(), 1.0e-9);
    }

    @Test
    void cotacaoInvalidaDoProvedorEhIgnorada() {
        repositorio.salvar(new Acao("PETR4", "Petrobras", 10.0, 30.0));
        ProvedorCotacao provedor = ticker -> -1.0;

        new MonitorMercadoService(provedor, repositorio).executarCiclo();

        assertEquals(30.0, repositorio.buscar("PETR4").getCotacaoAtual(), 1.0e-9);
    }

    @Test
    void trocarOProvedorValeParaOProximoCiclo() {
        repositorio.salvar(new Acao("PETR4", "Petrobras", 10.0, 30.0));
        MonitorMercadoService monitor = new MonitorMercadoService(ticker -> 31.0, repositorio);
        monitor.executarCiclo();

        monitor.setProvedor(ticker -> 40.0);
        monitor.executarCiclo();

        assertEquals(40.0, repositorio.buscar("PETR4").getCotacaoAtual(), 1.0e-9);
    }

    /*
      Usa um ticker que nenhum outro teste usa (ITSA4): o ciclo roda em outra thread e,
      se terminasse atrasado, poderia mexer num ativo de mesmo ticker criado pelo proximo teste.
    */
    @Test
    void iniciarRodaOPrimeiroCicloNaHoraEPararEncerra() throws Exception {
        repositorio.salvar(new Acao("ITSA4", "Itausa", 10.0, 10.0));
        MonitorMercadoService monitor = new MonitorMercadoService(ticker -> 11.0, repositorio);

        monitor.iniciar();
        boolean atualizou = esperarCotacao("ITSA4", 11.0, 3_000);
        monitor.parar();

        assertTrue(atualizou, "o primeiro ciclo deveria rodar logo ao iniciar");
        assertFalse(monitor.isRodando());
    }

    // Espera a thread do monitor atualizar a cotacao, por no maximo o tempo informado
    private boolean esperarCotacao(String ticker, double esperada, long limiteMs) throws InterruptedException {
        long fim = System.currentTimeMillis() + limiteMs;
        while (System.currentTimeMillis() < fim) {
            if (repositorio.buscar(ticker).getCotacaoAtual() == esperada) {
                return true;
            }
            Thread.sleep(20);
        }
        return false;
    }
}
