package br.ufrn.imd.investtrack.model;

import br.ufrn.imd.investtrack.interfaces.Tributavel;
import java.util.Locale;

// Criptoativo sem rendimento contratual embutido no modelo
public class Criptomoeda extends AtivoFinanceiro implements Tributavel {
    private static final double ALIQUOTA_ESTIMADA = 0.15;

    public Criptomoeda(String ticker, String nome, double quantidade, double precoMedio) {
        super(ticker, nome, quantidade, precoMedio);
    }

    // Retorna zero porque o modelo nao presume valorizacao futura de criptoativos
    @Override
    public double calcularRendimentoProjetado(int meses) {
        validarMeses(meses);
        return 0.0;
    }

    @Override
    public String avaliarCenario(double variacaoPct) {
        validarVariacaoPct(variacaoPct);
        String variacao = String.format(Locale.forLanguageTag("pt-BR"), "%+.2f%%", variacaoPct);

        if (Math.abs(variacaoPct) >= 8.0) {
            return "Oscilacao de " + variacao + " na criptomoeda: considere a volatilidade antes de decidir.";
        }
        return "Variacao de " + variacao + " na criptomoeda.";
    }

    // Estimativa didatica de 15% sobre o ganho positivo da posicao atual
    @Override
    public double calcularImposto() {
        return estimarImpostoSobreLucro(ALIQUOTA_ESTIMADA);
    }
}
