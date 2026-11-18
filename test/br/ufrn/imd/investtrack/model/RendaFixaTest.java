package br.ufrn.imd.investtrack.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import br.ufrn.imd.investtrack.exception.SaldoInsuficienteException;
import org.junit.jupiter.api.Test;

class RendaFixaTest {
    @Test
    void projetaRendimentoComTaxaAnualEfetiva() {
        RendaFixa rendaFixa = novaRendaFixa(1_000.0, 0.12);

        assertEquals(120.0, rendaFixa.calcularRendimentoProjetado(12), 1.0e-9);
    }

    @Test
    void aporteUsaCotacaoAtualEAtualizaQuantidade() {
        RendaFixa rendaFixa = novaRendaFixa(1_000.0, 0.10);
        rendaFixa.atualizarCotacao(1.10);

        rendaFixa.investir(110.0);

        assertEquals(1_100.0, rendaFixa.getQuantidade(), 1.0e-9);
        assertEquals(1.10, rendaFixa.getCotacaoAtual(), 1.0e-9);
    }

    @Test
    void resgataValorEmReaisERejeitaValorAcimaDoSaldo() throws Exception {
        RendaFixa rendaFixa = novaRendaFixa(1_000.0, 0.10);
        rendaFixa.atualizarCotacao(1.10);

        rendaFixa.resgatar(220.0);

        assertEquals(800.0, rendaFixa.getQuantidade(), 1.0e-9);
        assertEquals(880.0, rendaFixa.calcularValorAtual(), 1.0e-9);
        SaldoInsuficienteException excecao = assertThrows(
                SaldoInsuficienteException.class,
                () -> rendaFixa.resgatar(1_000.0));
        assertTrue(excecao.getMessage().contains("Saldo insuficiente"));
    }

    @Test
    void identificaPrazoEAliquotaInicial() {
        RendaFixa rendaFixa = novaRendaFixa(1_000.0, 0.10);

        assertTrue(rendaFixa.isAntesDoVencimento());
        assertEquals(0.225, rendaFixa.calcularAliquotaIR(), 1.0e-9);
    }

    @Test
    void calculaAliquotaComDataSimuladaSemEsperar() {
        RendaFixa rendaFixa = novaRendaFixa(1_000.0, 0.10);
        LocalDate dataAplicacao = rendaFixa.getDataAplicacao();

        assertEquals(
                0.20,
                rendaFixa.calcularAliquotaIR(dataAplicacao.plusDays(360)),
                1.0e-9);
        assertEquals(
                0.175,
                rendaFixa.calcularAliquotaIR(dataAplicacao.plusDays(361)),
                1.0e-9);
    }

    private static RendaFixa novaRendaFixa(double valor, double taxa) {
        return new RendaFixa("CDB teste", valor, taxa, LocalDate.now().plusYears(2));
    }

}
