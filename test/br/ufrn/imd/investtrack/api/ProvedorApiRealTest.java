package br.ufrn.imd.investtrack.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import br.ufrn.imd.investtrack.exception.CotacaoIndisponivelException;
import org.junit.jupiter.api.Test;

// Testes sem internet: so a leitura da resposta e as regras que nao dependem da rede
class ProvedorApiRealTest {
    @Test
    void extraiPrecoDaRespostaDaBinance() throws Exception {
        String resposta = "{\"symbol\":\"BTCBRL\",\"price\":\"612345.67000000\"}";

        assertEquals(612_345.67, ProvedorApiReal.extrairPrecoBinance(resposta), 1.0e-9);
    }

    @Test
    void respostaDeErroDaBinanceLancaCotacaoIndisponivel() {
        String erro = "{\"code\":-1121,\"msg\":\"Invalid symbol.\"}";

        assertThrows(CotacaoIndisponivelException.class, () -> ProvedorApiReal.extrairPrecoBinance(erro));
    }

    @Test
    void precoMalformadoOuZeradoLancaCotacaoIndisponivel() {
        assertThrows(CotacaoIndisponivelException.class,
                () -> ProvedorApiReal.extrairPrecoBinance("{\"price\":\"1.2.3\"}"));
        assertThrows(CotacaoIndisponivelException.class,
                () -> ProvedorApiReal.extrairPrecoBinance("{\"price\":\"0.00000000\"}"));
    }

    @Test
    void acaoAindaNaoTemApiELancaCotacaoIndisponivel() {
        ProvedorApiReal provedor = new ProvedorApiReal();

        assertThrows(CotacaoIndisponivelException.class, () -> provedor.buscarCotacaoAtual("PETR4"));
    }
}
