package br.ufrn.imd.investtrack.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.Set;
import br.ufrn.imd.investtrack.model.Acao;
import br.ufrn.imd.investtrack.model.AtivoFinanceiro;
import br.ufrn.imd.investtrack.model.Criptomoeda;
import br.ufrn.imd.investtrack.model.RendaFixa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CarteiraRepositoryTest {
    private CarteiraRepository repositorio;

    // O Singleton eh compartilhado entre os testes, entao a carteira comeca vazia em cada um
    @BeforeEach
    void esvaziarCarteira() {
        repositorio = CarteiraRepository.getInstancia();
        for (AtivoFinanceiro ativo : repositorio.listarTodos()) {
            repositorio.remover(ativo.getTicker());
        }
    }

    @Test
    void getInstanciaSempreDevolveOMesmoObjeto() {
        assertSame(CarteiraRepository.getInstancia(), CarteiraRepository.getInstancia());
    }

    @Test
    void construtorPrivadoImpedeCriarOutraCarteira() throws Exception {
        int modificadores = CarteiraRepository.class.getDeclaredConstructor().getModifiers();

        assertTrue(Modifier.isPrivate(modificadores));
    }

    @Test
    void salvaEBuscaPeloTickerSemDiferenciarMaiusculas() {
        Acao acao = new Acao("PETR4", "Petrobras", 10.0, 30.0);

        repositorio.salvar(acao);

        assertSame(acao, repositorio.buscar(" petr4 "));
    }

    @Test
    void buscarTickerQueNaoEstaNaCarteiraDevolveNull() {
        assertNull(repositorio.buscar("VALE3"));
        assertNull(repositorio.buscar(null));
    }

    @Test
    void removerTiraOAtivoDaCarteira() {
        repositorio.salvar(new Acao("VALE3", "Vale", 5.0, 60.0));

        repositorio.remover("VALE3");

        assertNull(repositorio.buscar("VALE3"));
        assertEquals(0, repositorio.listarTodos().size());
    }

    @Test
    void tickersMonitoradosNaoIncluemRendaFixa() {
        repositorio.salvar(new Acao("PETR4", "Petrobras", 10.0, 30.0));
        repositorio.salvar(new Criptomoeda("BTCBRL", "Bitcoin", 0.01, 300_000.0));
        repositorio.salvar(new RendaFixa("CDB Banco X", 1_000.0, 0.12, LocalDate.now().plusYears(1)));

        Set<String> tickers = repositorio.listarTickersMonitorados();

        assertEquals(Set.of("PETR4", "BTCBRL"), tickers);
    }

    @Test
    void listaTodosOsAtivosTributaveis() {
        repositorio.salvar(new Acao("PETR4", "Petrobras", 10.0, 30.0));
        repositorio.salvar(new Criptomoeda("BTCBRL", "Bitcoin", 0.01, 300_000.0));
        repositorio.salvar(new RendaFixa("CDB Banco X", 1_000.0, 0.12, LocalDate.now().plusYears(1)));

        assertEquals(3, repositorio.listarTributaveis().size());
    }
}
