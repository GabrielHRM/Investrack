package br.ufrn.imd.investtrack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import java.util.List;
import br.ufrn.imd.investtrack.interfaces.Tributavel;
import br.ufrn.imd.investtrack.model.Acao;
import br.ufrn.imd.investtrack.model.Criptomoeda;
import br.ufrn.imd.investtrack.model.RendaFixa;
import org.junit.jupiter.api.Test;

class CalculadoraIRServiceTest {
    private final CalculadoraIRService calculadora = new CalculadoraIRService();

    @Test
    void somaOImpostoQueCadaItemCalcula() {
        Acao acao = new Acao("PETR4", "Petrobras", 10.0, 100.0);
        acao.atualizarCotacao(120.0);
        Criptomoeda cripto = new Criptomoeda("BTCBRL", "Bitcoin", 2.0, 100.0);
        cripto.atualizarCotacao(120.0);
        RendaFixa rendaFixa = new RendaFixa("CDB Banco X", 1_000.0, 0.12, LocalDate.now().plusYears(1));

        double total = calculadora.calcularImpostoTotal(List.of(acao, cripto, rendaFixa));

        assertEquals(36.0, total, 1.0e-9);
    }

    @Test
    void ativoComPrejuizoNaoGeraImposto() {
        Acao acao = new Acao("VALE3", "Vale", 10.0, 100.0);
        acao.atualizarCotacao(80.0);

        assertEquals(0.0, calculadora.calcularImpostoTotal(List.of(acao)), 1.0e-9);
    }

    @Test
    void listaVaziaDaImpostoZero() {
        assertEquals(0.0, calculadora.calcularImpostoTotal(List.of()), 1.0e-9);
    }

    @Test
    void relatorioTemUmaLinhaPorItemEOTotal() {
        Acao acao = new Acao("PETR4", "Petrobras", 10.0, 100.0);
        acao.atualizarCotacao(120.0);
        Criptomoeda cripto = new Criptomoeda("BTCBRL", "Bitcoin", 2.0, 100.0);
        cripto.atualizarCotacao(120.0);
        List<Tributavel> itens = List.of(acao, cripto);

        String relatorio = calculadora.gerarRelatorio(itens);

        assertTrue(relatorio.contains("PETR4: R$ 30,00"));
        assertTrue(relatorio.contains("BTCBRL: R$ 6,00"));
        assertTrue(relatorio.contains("Total: R$ 36,00"));
    }
}
