package br.ufrn.imd.investtrack.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
import br.ufrn.imd.investtrack.exception.TickerInvalidoException;
import br.ufrn.imd.investtrack.model.Acao;
import br.ufrn.imd.investtrack.model.AtivoFinanceiro;
import br.ufrn.imd.investtrack.model.Criptomoeda;
import br.ufrn.imd.investtrack.model.RendaFixa;
import br.ufrn.imd.investtrack.model.TipoAtivo;
import org.junit.jupiter.api.Test;

class AtivoFactoryTest {
    @Test
    void criaAcaoComTickerNormalizado() throws Exception {
        AtivoFinanceiro ativo = AtivoFactory.criarAtivo(TipoAtivo.ACAO, " petr4 ", "Petrobras", 10.0, 30.0);

        assertInstanceOf(Acao.class, ativo);
        assertEquals("PETR4", ativo.getTicker());
        assertEquals(10.0, ativo.getQuantidade(), 1.0e-9);
        assertEquals(30.0, ativo.getPrecoMedio(), 1.0e-9);
    }

    @Test
    void aceitaAcaoComDoisDigitos() throws Exception {
        AtivoFinanceiro ativo = AtivoFactory.criarAtivo(TipoAtivo.ACAO, "TAEE11", "Taesa", 3.0, 35.0);

        assertEquals("TAEE11", ativo.getTicker());
    }

    @Test
    void criaCriptomoeda() throws Exception {
        AtivoFinanceiro ativo = AtivoFactory.criarAtivo(TipoAtivo.CRIPTOMOEDA, "btcbrl", "Bitcoin", 0.5, 300_000.0);

        assertInstanceOf(Criptomoeda.class, ativo);
        assertEquals("BTCBRL", ativo.getTicker());
    }

    @Test
    void rejeitaTickerDeAcaoForaDoFormato() {
        String[] tickersInvalidos = { "PETR", "PETR444", "4PETR", "BTCBRL", "", "   ", null };

        for (String ticker : tickersInvalidos) {
            assertThrows(
                    TickerInvalidoException.class,
                    () -> AtivoFactory.criarAtivo(TipoAtivo.ACAO, ticker, "Teste", 1.0, 10.0),
                    "deveria rejeitar o ticker de acao: " + ticker);
        }
    }

    @Test
    void rejeitaTickerDeCriptomoedaForaDoFormato() {
        String[] tickersInvalidos = { "BTC", "PETR4", "BTCUSDT", "BRL" };

        for (String ticker : tickersInvalidos) {
            assertThrows(
                    TickerInvalidoException.class,
                    () -> AtivoFactory.criarAtivo(TipoAtivo.CRIPTOMOEDA, ticker, "Teste", 1.0, 10.0),
                    "deveria rejeitar o ticker de cripto: " + ticker);
        }
    }

    @Test
    void rejeitaQuantidadeOuPrecoQueNaoSejamPositivos() {
        assertThrows(IllegalArgumentException.class,
                () -> AtivoFactory.criarAtivo(TipoAtivo.ACAO, "PETR4", "Petrobras", 0.0, 30.0));
        assertThrows(IllegalArgumentException.class,
                () -> AtivoFactory.criarAtivo(TipoAtivo.ACAO, "PETR4", "Petrobras", 10.0, -1.0));
    }

    @Test
    void rendaFixaNaoEhCriadaPeloCriarAtivo() {
        assertThrows(IllegalArgumentException.class,
                () -> AtivoFactory.criarAtivo(TipoAtivo.RENDA_FIXA, "CDB", "CDB", 1_000.0, 1.0));
    }

    @Test
    void criaRendaFixaComValorEmReais() {
        RendaFixa rendaFixa = AtivoFactory.criarRendaFixa("CDB Banco X", 1_000.0, 0.12, LocalDate.now().plusYears(2));

        assertEquals(1_000.0, rendaFixa.calcularValorAtual(), 1.0e-9);
        assertEquals(0.12, rendaFixa.getTaxaAnual(), 1.0e-9);
    }
}
