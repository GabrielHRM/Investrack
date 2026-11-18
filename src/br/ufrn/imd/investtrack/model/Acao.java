package br.ufrn.imd.investtrack.model;

import br.ufrn.imd.investtrack.interfaces.Tributavel;
import java.util.Locale;

/* Acao com projecao simples de dividendos. O dividend yield usa fracao anual: 0.05 = 5%. */
public class Acao extends AtivoFinanceiro implements Tributavel{
    private static final double ALIQUOTA_ESTIMADA = 0.15;

    private final double dividendYieldAnual;

    /* Usado quando a criacao do ativo nao recebe um dividend yield informado. */
    public Acao(String ticker, String nome, double quantidade, double precoMedio) {
        this(ticker, nome, quantidade, precoMedio, 0.0);
    }

    public Acao(String ticker, String nome, double quantidade, double precoMedio, double dividendYieldAnual) {
        super(ticker, nome, quantidade, precoMedio);
        this.dividendYieldAnual = validarNaoNegativo(dividendYieldAnual, "dividend yield anual");
    }

    @Override
    public synchronized double calcularRendimentoProjetado(int meses){
        validarMeses(meses);
        return calcularValorAtual() * dividendYieldAnual * meses / 12.0;
    }

    // funcaoo que avalia o cenario do mercado de acoes pela funcao herdada de AtivoFinanceiro
    @Override
    public String avaliarCenario(double variacaoPct){
        validarVariacaoPct(variacaoPct);
        String variacao = formatarVariacao(variacaoPct);

        if (variacaoPct <= -5.0) {
            return "Queda de " + variacao + ": avalie os fundamentos da acao antes de decidir.";
        }
        if (variacaoPct >= 5.0) {
            return "Alta de " + variacao + ": considere o risco antes de aumentar a exposicao.";
        }
        return "Variacao de " + variacao + " na acao.";
    }

    /** Estimativa didatica de 15% sobre o ganho positivo da posicao atual. */
    @Override
    public double calcularImposto() {
        return estimarImpostoSobreLucro(ALIQUOTA_ESTIMADA);
    }

    public double getDividendYieldAnual() {
        return dividendYieldAnual;
    }

    private static String formatarVariacao(double variacaoPct) {
        return String.format(Locale.forLanguageTag("pt-BR"), "%+.2f%%", variacaoPct);
    }
}