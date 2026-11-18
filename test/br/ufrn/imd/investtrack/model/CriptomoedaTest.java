package br.ufrn.imd.investtrack.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class CriptomoedaTest {
    @Test
    void naoProjetaRendimentoContratual() {
        Criptomoeda cripto = new Criptomoeda("BTCBRL", "Bitcoin", 0.01, 300_000.0);

        assertEquals(0.0, cripto.calcularRendimentoProjetado(12), 1.0e-9);
    }

    @Test
    void estimaImpostoSobreLucroPositivoDaPosicao() {
        Criptomoeda cripto = new Criptomoeda("BTCBRL", "Bitcoin", 2.0, 100.0);
        cripto.atualizarCotacao(120.0);

        assertEquals(6.0, cripto.calcularImposto(), 1.0e-9);
    }

    @Test
    void sinalizaOscilacaoAlta() {
        Criptomoeda cripto = new Criptomoeda("BTCBRL", "Bitcoin", 1.0, 100.0);

        assertTrue(cripto.avaliarCenario(9.0).contains("volatilidade"));
    }
}