package br.ufrn.imd.investtrack.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import br.ufrn.imd.investtrack.exception.CotacaoIndisponivelException;
import br.ufrn.imd.investtrack.model.Acao;
import br.ufrn.imd.investtrack.model.AtivoFinanceiro;
import br.ufrn.imd.investtrack.repository.CarteiraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProvedorSimuladoTest {
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
    void variaNoMaximoDoisPorCentoAPartirDaCotacaoDaCarteira() throws Exception {
        repositorio.salvar(new Acao("PETR4", "Petrobras", 10.0, 100.0));
        ProvedorSimulado provedor = new ProvedorSimulado(repositorio, 42L);

        for (int i = 0; i < 500; i++) {
            double cotacao = provedor.buscarCotacaoAtual("PETR4");

            assertTrue(cotacao >= 98.0 && cotacao <= 102.0, "fora da faixa de 2%: " + cotacao);
        }
    }

    @Test
    void mesmaSementeGeraOsMesmosPrecos() throws Exception {
        repositorio.salvar(new Acao("PETR4", "Petrobras", 10.0, 100.0));
        ProvedorSimulado primeiro = new ProvedorSimulado(repositorio, 7L);
        ProvedorSimulado segundo = new ProvedorSimulado(repositorio, 7L);

        for (int i = 0; i < 10; i++) {
            assertEquals(primeiro.buscarCotacaoAtual("PETR4"), segundo.buscarCotacaoAtual("PETR4"), 1.0e-9);
        }
    }

    @Test
    void tickerForaDaCarteiraNaoTemPrecoDeReferencia() {
        ProvedorSimulado provedor = new ProvedorSimulado(repositorio, 1L);

        assertThrows(CotacaoIndisponivelException.class, () -> provedor.buscarCotacaoAtual("VALE3"));
    }
}
