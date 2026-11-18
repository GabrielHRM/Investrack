package br.ufrn.imd.investtrack.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class AcaoTest {
    @Test
    void compraAtualizaPrecoMedioPonderadoSemTrocarCotacao() {
        Acao acao = new Acao("vale3", "Vale", 2.0, 100.0, 0.05);

        acao.comprar(3.0, 130.0);

        assertEquals("VALE3", acao.getTicker());
        assertEquals(5.0, acao.getQuantidade(), 1.0e-9);
        assertEquals(118.0, acao.getPrecoMedio(), 1.0e-9);
        assertEquals(100.0, acao.getCotacaoAtual(), 1.0e-9);
    }

    @Test
    void projetaDividendosDeFormaProporcionalAoPrazo() {
        Acao acao = new Acao("ITUB4", "Itaú", 10.0, 100.0, 0.06);

        assertEquals(30.0, acao.calcularRendimentoProjetado(6), 1.0e-9);
    }

    @Test
    void estimaImpostoSobreLucroPositivoDaPosicao() {
        Acao acao = new Acao("PETR4", "Petrobras", 10.0, 100.0, 0.0);
        acao.atualizarCotacao(120.0);

        assertEquals(30.0, acao.calcularImposto(), 1.0e-9);
    }

    @Test
    void descreveQuedaAcentuada() {
        Acao acao = new Acao("VALE3", "Vale", 1.0, 100.0, 0.0);

        assertTrue(acao.avaliarCenario(-6.0).contains("Queda"));
    }
}