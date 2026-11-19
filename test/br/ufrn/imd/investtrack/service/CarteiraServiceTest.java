package br.ufrn.imd.investtrack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import br.ufrn.imd.investtrack.exception.AtivoNaoEncontradoException;
import br.ufrn.imd.investtrack.exception.SaldoInsuficienteException;
import br.ufrn.imd.investtrack.exception.TickerInvalidoException;
import br.ufrn.imd.investtrack.model.Acao;
import br.ufrn.imd.investtrack.model.AtivoFinanceiro;
import br.ufrn.imd.investtrack.model.TipoAtivo;
import br.ufrn.imd.investtrack.repository.CarteiraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CarteiraServiceTest {
    private CarteiraRepository repositorio;
    private CarteiraService service;

    // O Singleton eh compartilhado entre os testes, entao a carteira comeca vazia em cada um
    @BeforeEach
    void prepararCarteiraVazia() {
        repositorio = CarteiraRepository.getInstancia();
        for (AtivoFinanceiro ativo : repositorio.listarTodos()) {
            repositorio.remover(ativo.getTicker());
        }
        service = new CarteiraService(repositorio);
    }

    @Test
    void compraDeAtivoNovoSalvaNaCarteira() throws Exception {
        service.comprar(TipoAtivo.ACAO, "PETR4", "Petrobras", 10.0, 30.0);

        AtivoFinanceiro ativo = repositorio.buscar("PETR4");
        assertInstanceOf(Acao.class, ativo);
        assertEquals(10.0, ativo.getQuantidade(), 1.0e-9);
    }

    @Test
    void segundaCompraSomaQuantidadeERecalculaPrecoMedio() throws Exception {
        service.comprar(TipoAtivo.ACAO, "VALE3", "Vale", 2.0, 100.0);

        service.comprar(TipoAtivo.ACAO, "vale3", "Vale", 3.0, 130.0);

        AtivoFinanceiro ativo = repositorio.buscar("VALE3");
        assertEquals(5.0, ativo.getQuantidade(), 1.0e-9);
        assertEquals(118.0, ativo.getPrecoMedio(), 1.0e-9);
        assertEquals(1, repositorio.listarTodos().size());
    }

    @Test
    void compraComTickerInvalidoNaoSalvaNada() {
        assertThrows(
                TickerInvalidoException.class,
                () -> service.comprar(TipoAtivo.ACAO, "PETROBRAS", "Petrobras", 10.0, 30.0));

        assertEquals(0, repositorio.listarTodos().size());
    }

    @Test
    void compraComTipoDiferenteDoAtivoExistenteLancaExcecao() throws Exception {
        service.comprar(TipoAtivo.CRIPTOMOEDA, "BTCBRL", "Bitcoin", 0.01, 300_000.0);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.comprar(TipoAtivo.ACAO, "BTCBRL", "Bitcoin", 1.0, 300_000.0));
        assertEquals(0.01, repositorio.buscar("BTCBRL").getQuantidade(), 1.0e-9);
    }

    @Test
    void vendaParcialMantemAtivoNaCarteira() throws Exception {
        service.comprar(TipoAtivo.ACAO, "ITUB4", "Itau", 5.0, 20.0);

        service.vender("ITUB4", 2.0);

        assertEquals(3.0, repositorio.buscar("ITUB4").getQuantidade(), 1.0e-9);
    }

    @Test
    void vendaTotalRemoveAtivoDaCarteira() throws Exception {
        service.comprar(TipoAtivo.ACAO, "ITUB4", "Itau", 5.0, 20.0);

        service.vender("ITUB4", 5.0);

        assertNull(repositorio.buscar("ITUB4"));
    }

    @Test
    void vendaDeAtivoQueNaoEstaNaCarteiraLancaExcecao() {
        AtivoNaoEncontradoException excecao = assertThrows(
                AtivoNaoEncontradoException.class,
                () -> service.vender("MGLU3", 1.0));

        assertTrue(excecao.getMessage().contains("MGLU3"));
    }

    @Test
    void vendaAcimaDoSaldoLancaExcecaoEMantemPosicao() throws Exception {
        service.comprar(TipoAtivo.ACAO, "ITUB4", "Itau", 2.0, 20.0);

        assertThrows(SaldoInsuficienteException.class, () -> service.vender("ITUB4", 3.0));

        assertEquals(2.0, repositorio.buscar("ITUB4").getQuantidade(), 1.0e-9);
    }

    @Test
    void novoInvestimentoNaMesmaRendaFixaSomaAoValor() {
        LocalDate vencimento = LocalDate.now().plusYears(2);
        service.investirRendaFixa("CDB Banco X", 1_000.0, 0.12, vencimento);

        service.investirRendaFixa("CDB Banco X", 500.0, 0.12, vencimento);

        assertEquals(1_500.0, repositorio.buscar("CDB Banco X").calcularValorAtual(), 1.0e-9);
        assertEquals(1, repositorio.listarTodos().size());
    }

    @Test
    void resgateAntesDoVencimentoAvisaAliquotaDeIR() throws Exception {
        service.investirRendaFixa("CDB Banco X", 1_000.0, 0.12, LocalDate.now().plusYears(2));

        String mensagem = service.resgatarRendaFixa("CDB Banco X", 400.0);

        assertTrue(mensagem.contains("antes do vencimento"));
        assertTrue(mensagem.contains("22,5%"));
        assertEquals(600.0, repositorio.buscar("CDB Banco X").calcularValorAtual(), 1.0e-9);
    }

    @Test
    void resgateDoValorTodoRemoveARendaFixa() throws Exception {
        service.investirRendaFixa("CDB Banco X", 1_000.0, 0.12, LocalDate.now().plusYears(2));

        service.resgatarRendaFixa("CDB Banco X", 1_000.0);

        assertNull(repositorio.buscar("CDB Banco X"));
    }

    @Test
    void resgateDeAcaoComoRendaFixaLancaExcecao() throws Exception {
        service.comprar(TipoAtivo.ACAO, "PETR4", "Petrobras", 10.0, 30.0);

        assertThrows(IllegalArgumentException.class, () -> service.resgatarRendaFixa("PETR4", 10.0));
    }

    @Test
    void calculaPatrimonioELucroTotaisDaCarteira() throws Exception {
        service.comprar(TipoAtivo.ACAO, "PETR4", "Petrobras", 10.0, 100.0);
        service.comprar(TipoAtivo.CRIPTOMOEDA, "BTCBRL", "Bitcoin", 2.0, 50.0);
        repositorio.buscar("PETR4").atualizarCotacao(120.0);
        repositorio.buscar("BTCBRL").atualizarCotacao(40.0);

        assertEquals(1_280.0, service.calcularPatrimonioTotal(), 1.0e-9);
        assertEquals(180.0, service.calcularLucroPrejuizoTotal(), 1.0e-9);
    }
}
