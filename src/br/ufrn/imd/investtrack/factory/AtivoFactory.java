package br.ufrn.imd.investtrack.factory;

import br.ufrn.imd.investtrack.exception.TickerInvalidoException;
import br.ufrn.imd.investtrack.model.Acao;
import br.ufrn.imd.investtrack.model.AtivoFinanceiro;
import br.ufrn.imd.investtrack.model.Criptomoeda;
import br.ufrn.imd.investtrack.model.RendaFixa;
import br.ufrn.imd.investtrack.model.TipoAtivo;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

/*
 Factory Method: centraliza a criacao dos ativos. Quem usa (CarteiraService) so informa o tipo
 e recebe um AtivoFinanceiro, sem saber qual subclasse foi criada. Para adicionar um tipo novo
 (ex.: fundo imobiliario), basta mexer aqui e no TipoAtivo.
*/
public final class AtivoFactory {
    // Acao da B3: 4 letras e 1 ou 2 numeros, como PETR4 e TAEE11
    private static final String FORMATO_ACAO = "[A-Z]{4}[0-9]{1,2}";
    // Par da Binance cotado em reais, como BTCBRL e ETHBRL
    private static final String FORMATO_CRIPTO = "[A-Z0-9]{2,10}BRL";

    private AtivoFactory() {
        // classe so com metodos static, nao precisa de objeto
    }

    // Cria uma acao ou uma criptomoeda. Renda fixa tem dados diferentes e usa criarRendaFixa
    public static AtivoFinanceiro criarAtivo(TipoAtivo tipo, String ticker, String nome, double quantidade, double preco)
            throws TickerInvalidoException {
        Objects.requireNonNull(tipo, "tipo de ativo nao pode ser nulo");
        validarTicker(tipo, ticker);
        validarPositivo(quantidade, "quantidade");
        validarPositivo(preco, "preco");

        switch (tipo) {
            case ACAO:
                return new Acao(ticker, nome, quantidade, preco);
            case CRIPTOMOEDA:
                return new Criptomoeda(ticker, nome, quantidade, preco);
            default:
                throw new IllegalArgumentException("Renda fixa deve ser criada por criarRendaFixa.");
        }
    }

    public static RendaFixa criarRendaFixa(String nome, double valor, double taxaAnual, LocalDate vencimento) {
        return new RendaFixa(nome, valor, taxaAnual, vencimento);
    }

    private static void validarTicker(TipoAtivo tipo, String ticker) throws TickerInvalidoException {
        if (ticker == null || ticker.isBlank()) {
            throw new TickerInvalidoException("Ticker nao pode ficar vazio.");
        }
        String valor = ticker.trim().toUpperCase(Locale.ROOT);

        if (tipo == TipoAtivo.ACAO && !valor.matches(FORMATO_ACAO)) {
            throw new TickerInvalidoException("Ticker de acao invalido: " + valor + ". Exemplo valido: PETR4.");
        }
        if (tipo == TipoAtivo.CRIPTOMOEDA && !valor.matches(FORMATO_CRIPTO)) {
            throw new TickerInvalidoException("Ticker de criptomoeda invalido: " + valor + ". Exemplo valido: BTCBRL.");
        }
    }

    // Na criacao a quantidade tem que ser maior que zero (o construtor do ativo aceita zero)
    private static void validarPositivo(double valor, String campo) {
        if (!Double.isFinite(valor) || valor <= 0.0) {
            throw new IllegalArgumentException(campo + " deve ser um numero positivo e finito.");
        }
    }
}
