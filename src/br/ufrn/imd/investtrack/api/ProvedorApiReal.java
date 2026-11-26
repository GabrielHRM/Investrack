package br.ufrn.imd.investtrack.api;

import br.ufrn.imd.investtrack.exception.CotacaoIndisponivelException;
import br.ufrn.imd.investtrack.interfaces.ProvedorCotacao;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 Busca cotacoes reais na internet com o HttpClient do Java (java.net.http).
 Por enquanto so as criptomoedas, pela API publica da Binance (nao precisa de chave).
 Acoes (brapi) e cambio (AwesomeAPI) entram nos proximos passos.
*/
public class ProvedorApiReal implements ProvedorCotacao {
    private static final String URL_BINANCE = "https://api.binance.com/api/v3/ticker/price?symbol=";
    // A Binance responde assim: {"symbol":"BTCBRL","price":"612345.67000000"}
    private static final Pattern PRECO_BINANCE = Pattern.compile("\"price\"\\s*:\\s*\"([0-9.]+)\"");

    private final HttpClient cliente;

    public ProvedorApiReal() {
        this.cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public double buscarCotacaoAtual(String ticker) throws CotacaoIndisponivelException {
        // Os tickers de cripto da carteira sao pares da Binance em reais, como BTCBRL (ver AtivoFactory)
        if (ticker.endsWith("BRL")) {
            return buscarNaBinance(ticker);
        }
        throw new CotacaoIndisponivelException("Cotacao de " + ticker + " ainda nao eh buscada na internet.");
    }

    private double buscarNaBinance(String ticker) throws CotacaoIndisponivelException {
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(URL_BINANCE + ticker))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        try {
            HttpResponse<String> resposta = cliente.send(pedido, HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() != 200) {
                throw new CotacaoIndisponivelException(
                        "Binance respondeu HTTP " + resposta.statusCode() + " para " + ticker + ".");
            }
            return extrairPrecoBinance(resposta.body());
        } catch (IOException erro) {
            throw new CotacaoIndisponivelException("Sem conexao com a Binance: " + erro.getMessage());
        } catch (InterruptedException erro) {
            Thread.currentThread().interrupt(); // o monitor foi parado no meio da consulta
            throw new CotacaoIndisponivelException("Consulta a Binance interrompida.");
        }
    }

    // Separado do HTTP para poder ser testado sem internet
    static double extrairPrecoBinance(String json) throws CotacaoIndisponivelException {
        Matcher m = PRECO_BINANCE.matcher(json);
        if (!m.find()) {
            throw new CotacaoIndisponivelException("Resposta da Binance sem preco: " + json);
        }
        double preco;
        try {
            preco = Double.parseDouble(m.group(1));
        } catch (NumberFormatException erro) {
            throw new CotacaoIndisponivelException("Binance devolveu um preco malformado: " + m.group(1));
        }
        if (preco <= 0.0) {
            throw new CotacaoIndisponivelException("Binance devolveu preco invalido: " + preco);
        }
        return preco;
    }
}
