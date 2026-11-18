package br.ufrn.imd.investtrack.model;

import br.ufrn.imd.investtrack.exception.SaldoInsuficienteException;
import br.ufrn.imd.investtrack.interfaces.Tributavel;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;

public class RendaFixa extends AtivoFinanceiro implements Tributavel{
    private final double taxaAnual;
    private final LocalDate dataAplicacao; // data que marca inicio da aplicacao de renda fixa
    private final LocalDate dataVencimento; // data final para que a aplicacao da renda fixa continue rendendo

    public RendaFixa(String nome, double valor, double taxaAnual, LocalDate dataVencimento){
        super(nome, nome, validarPositivo(valor, "valor investido"), 1.0);
        this.taxaAnual = validarNaoNegativo(taxaAnual, "taxa anual");
        this.dataAplicacao = LocalDate.now();
        this.dataVencimento = Objects.requireNonNull(dataVencimento, "data de vencimento nao pode ser nula");

        if (this.dataVencimento.isBefore(this.dataAplicacao)) {
            throw new IllegalArgumentException("A data de vencimento nao pode estar no passado.");
        }
    }

    // Adiciona dinheiro ah aplicacao. O valor eh em reais./
    public synchronized void investir(double valor) {
        double valorInvestido = validarPositivo(valor, "valor investido");
        double cotacao = getCotacaoAtual();
        super.comprar(valorInvestido / cotacao, cotacao);
    }

    // Resgtae do investimento aplicado
    /*
    Retira um valor em reais da posicao. Se tentar retirar mais do que a posicao vale, 
    o metodo lanca um erro de saldo insuficiente 
    */
    public synchronized void resgatar(double valor) throws SaldoInsuficienteException {
        double valorResgatado = validarPositivo(valor, "valor de resgate");
        double cotacao = getCotacaoAtual();
        double valorDisponivel = calcularValorAtual();
        if (valorResgatado > valorDisponivel) {
            throw new SaldoInsuficienteException(String.format(
                    Locale.forLanguageTag("pt-BR"),
                    "Saldo insuficiente para resgate: solicitado R$ %.2f, disponível R$ %.2f.",
                    valorResgatado, valorDisponivel));
        }
        super.vender(valorResgatado / cotacao);
    }

    // Estima o valor do rendimento em reais para o prazo informado, usando a taxa anual da aplicacao
    @Override
    public synchronized double calcularRendimentoProjetado(int meses) {
        validarMeses(meses);
        double valorAtual = calcularValorAtual();
        double valorProjetado = valorAtual * Math.pow(1.0 + taxaAnual, meses / 12.0);
        return valorProjetado - valorAtual;
    }

    @Override
    public String avaliarCenario(double variacaoPct) {
        validarVariacaoPct(variacaoPct);
        return String.format(Locale.forLanguageTag("pt-BR"), "Variacao de %+.2f%%. Confira as condicoes do titulo e o prazo de resgate.",variacaoPct);
    }

    // Aliquota regressiva estimada para aplicacoes tributaveis de longo prazo
    public double calcularAliquotaIR() {
        return calcularAliquotaIR(LocalDate.now());
    }

    /*
    Calcula a aliquota usando uma data de referencia escolhida.
    Permite simular o tempo decorrido sem alterar a data real da aplicacao.
     */
    double calcularAliquotaIR(LocalDate dataReferencia) {
        Objects.requireNonNull(dataReferencia, "data de referencia nao pode ser nula");
        if (dataReferencia.isBefore(dataAplicacao)) {
            throw new IllegalArgumentException("A data de referencia nao pode ser anterior ah aplicacao.");
        }

        long dias = ChronoUnit.DAYS.between(dataAplicacao, dataReferencia);
        if (dias <= 180) {
            return 0.225;
        }
        if (dias <= 360) {
            return 0.20;
        }
        if (dias <= 720) {
            return 0.175;
        }
        return 0.15;
    }

    public boolean isAntesDoVencimento() {
        return LocalDate.now().isBefore(dataVencimento);
    }

    // Estimativa do imposto sobre o ganho atual da posicao, se resgatada agora
    @Override
    public double calcularImposto() {
        return estimarImpostoSobreLucro(calcularAliquotaIR());
    }

    public double getTaxaAnual() {
        return taxaAnual;
    }

    public LocalDate getDataAplicacao() {
        return dataAplicacao;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

}