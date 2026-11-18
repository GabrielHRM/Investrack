package br.ufrn.imd.investtrack.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class AtivoFinanceiroTest {
    @Test
    void vendaMantemPrecoMedioEAtualizaPosicao() throws Exception {
        Acao acao = new Acao("ITUB4", "Itaú", 5.0, 20.0, 0.0);

        acao.vender(2.0);

        assertEquals(3.0, acao.getQuantidade(), 1.0e-9);
        assertEquals(20.0, acao.getPrecoMedio(), 1.0e-9);
    }

    @Test
    void vendaAcimaDoSaldoLancaExcecaoDeDominio() {
        Acao acao = new Acao("ITUB4", "Itaú", 2.0, 20.0, 0.0);

        assertThrows(
                br.ufrn.imd.investtrack.exception.SaldoInsuficienteException.class,
                () -> acao.vender(3.0));
    }

    @Test
    void zerarPosicaoPermiteNovaCompraComNovoPrecoMedio() throws Exception {
        Acao acao = new Acao("ITUB4", "Itaú", 2.0, 20.0, 0.0);
        acao.vender(2.0);

        acao.comprar(1.0, 25.0);

        assertEquals(1.0, acao.getQuantidade(), 1.0e-9);
        assertEquals(25.0, acao.getPrecoMedio(), 1.0e-9);
        assertEquals(25.0, acao.getCotacaoAtual(), 1.0e-9);
    }
}