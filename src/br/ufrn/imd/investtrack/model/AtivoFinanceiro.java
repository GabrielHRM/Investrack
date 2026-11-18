package br.ufrn.imd.investtrack.model;
import br.ufrn.imd.investtrack.exception.SaldoInsuficienteException;

import java.util.Locale;
import java.util.Objects;

public abstract class AtivoFinanceiro{
    private final String ticker; // codigo do ativo, como PETR4 ou BTCBRL
    private final String nome; // nome descritivo, como Petrobras ou Bitcoin
    private double quantidade; // quanto o investidor possui de determinado ativo(cripto moeda, renda fixa, acoes e ect..)
    private double precoMedio; // custo medio de compra de ativo por unidade
    private double cotacaoAtual; // valor de 1 unidade de ativo no mercado

    protected AtivoFinanceiro(String ticker, String nome, double quantidade, double precoMedio) {
        this.ticker = validarTicker(ticker);
        this.nome = validarNome(nome);
        this.quantidade = validarNaoNegativo(quantidade, "quantidade");
        this.precoMedio = validarNaoNegativo(precoMedio, "preco medio");
        this.cotacaoAtual = precoMedio;
    }

    /*
      esse mecanismo sincroniza com o objeto, protejendo-o de atualizacoes ate que a nova cotacao seja definida, 
      para so entao alterar o objeto cotacaoAtual com o valor atual do ativo por unidade.

      A cada ciclo, o monitor busca a cotacao nova. Ao chamar atualizarCotacao, o synchronized reserva brevemente o objeto
      enquanto o campo cotacaoAtual recebe o preco por unidade. Durante essa alteracao, outras threads que chamem 
      metodos synchronized do mesmo ativo esperam. Quando a atualizacao termina, elas podem continuar.
    */
    public synchronized void atualizarCotacao(double novaCotacao) {
        this.cotacaoAtual = validarPositivo(novaCotacao, "cotacao");
    }

    /*
      Funcao para atualizar a quantidade de ativos do usuario, com base no que ele ja tem e que comprou,
      alem de atualizar o preco medio do ativo por unidade possuida.
      Como o metodo e synchronized, outras chamadas sincronizadas sobre uma mesma instancia aguardam a compra terminar.
     */
    public synchronized void comprar(double quantidade, double preco) {
        double quantidadeComprada = validarPositivo(quantidade, "quantidade comprada");
        double precoCompra = validarPositivo(preco, "preco de compra");

        double quantidadeAnterior = this.quantidade;
        double quantidadeTotal = quantidadeAnterior + quantidadeComprada;

        // Calculo do preco medio dos ativos por unidades compradas e possuidas previamente
        this.precoMedio = quantidadeAnterior == 0.0 ? precoCompra : ((quantidadeAnterior * this.precoMedio) + (quantidadeComprada * precoCompra)) / quantidadeTotal;
        this.quantidade = quantidadeTotal;

        if (quantidadeAnterior == 0.0) {
            this.cotacaoAtual = precoCompra;
        }
    }


    /*
       Metodo para venda de ativos em posse do ususario. 
       Atraves do trhow ele verifica se o usuario possui ativos suficiente para realizar uma venda, 
       lanca uma mensagem de que a quantidade do ativo eh insuficenter(saldo insuficiente) e,
       caso essas condicoes nao sejam atenddias, deduz dacarteira do usuario a quantidade de ativos vendidos 
    */
    public synchronized void vender(double quantidade) throws SaldoInsuficienteException {
        double quantidadeVendida = validarPositivo(quantidade, "quantidade vendida");
        if (quantidadeVendida > this.quantidade) {
            throw new SaldoInsuficienteException(String.format(Locale.forLanguageTag("pt-BR"),
                    "Saldo insuficiente de %s: solicitado %.8f, disponivel %.8f.",
                    ticker, quantidadeVendida, this.quantidade));
        }

        this.quantidade -= quantidadeVendida;
        if (this.quantidade == 0.0) {
            this.precoMedio = 0.0;
        }
    }

    // Metodo que retorna o valor de um ativo atual em posse do usuario pela quantidade dele e a cotacao de mercado atual
    public synchronized double calcularValorAtual() {
        return this.quantidade * this.cotacaoAtual;
    }

    /** Lucro ou prejuizo ainda nao realizado na posicao mantida. */
    public synchronized double calcularLucroPrejuizo() {
        return this.quantidade * (this.cotacaoAtual - this.precoMedio);
    }

    /*
      Estima o imposto como se a posicaoo atual fosse liquidada agora.
      Nao inclui isencoes, custos, modalidade da operacao nem vendas anteriores.
     */
    protected synchronized double estimarImpostoSobreLucro(double aliquota) {
        return Math.max(0.0, calcularLucroPrejuizo()) * aliquota;
    }

    // Projeta o rendimento para os proximos meses, conforme a regra de cada tipo de ativo
    public abstract double calcularRendimentoProjetado(int meses);

    // recebe a variacao percentual e retorna uma mensagem descrevendo o cenario de, por exemplo, alta ou queda de um ativo
    public abstract String avaliarCenario(double variacaoPct);

    public String getTicker() {
        return ticker;
    }

    public String getNome() {
        return nome;
    }

    public synchronized double getQuantidade() {
        return quantidade;
    }

    public synchronized double getPrecoMedio() {
        return precoMedio;
    }

    public synchronized double getCotacaoAtual() {
        return cotacaoAtual;
    }


    protected static double validarNaoNegativo(double valor, String campo) {
        if (!Double.isFinite(valor) || valor < 0.0) {
            throw new IllegalArgumentException(campo + " deve ser um numero nao negativo e finito.");
        }
        return valor;
    }

    protected static double validarPositivo(double valor, String campo) {
        if (!Double.isFinite(valor) || valor <= 0.0) {
            throw new IllegalArgumentException(campo + " deve ser um numero positivo e finito.");
        }
        return valor;
    }

    protected static void validarMeses(int meses) {
        if (meses < 0) {
            throw new IllegalArgumentException("O numero de meses nao pode ser negativo.");
        }
    }

    protected static double validarVariacaoPct(double variacaoPct) {
        if (!Double.isFinite(variacaoPct)) {
            throw new IllegalArgumentException("A variacao percentual deve ser finita.");
        }
        return variacaoPct;
    }

    private static String validarTicker(String ticker) {
        String valor = Objects.requireNonNull(ticker, "ticker nao pode ser nulo").trim();
        if (valor.isEmpty()) {
            throw new IllegalArgumentException("Ticker nao pode ficar vazio.");
        }
        return valor.toUpperCase(Locale.ROOT);
    }

    private static String validarNome(String nome) {
        String valor = Objects.requireNonNull(nome, "nome nao pode ser nulo").trim();
        if (valor.isEmpty()) {
            throw new IllegalArgumentException("Nome nao pode ficar vazio.");
        }
        return valor;
    }

}
