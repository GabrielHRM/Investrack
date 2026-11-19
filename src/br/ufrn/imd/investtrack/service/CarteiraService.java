package br.ufrn.imd.investtrack.service;

import br.ufrn.imd.investtrack.exception.AtivoNaoEncontradoException;
import br.ufrn.imd.investtrack.exception.SaldoInsuficienteException;
import br.ufrn.imd.investtrack.exception.TickerInvalidoException;
import br.ufrn.imd.investtrack.factory.AtivoFactory;
import br.ufrn.imd.investtrack.model.Acao;
import br.ufrn.imd.investtrack.model.AtivoFinanceiro;
import br.ufrn.imd.investtrack.model.Criptomoeda;
import br.ufrn.imd.investtrack.model.RendaFixa;
import br.ufrn.imd.investtrack.model.TipoAtivo;
import br.ufrn.imd.investtrack.repository.CarteiraRepository;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

/*
 Coordena as operacoes da carteira pedidas pela tela: comprar, vender, investir e resgatar.
 A tela nunca mexe direto no repositorio, sempre passa por aqui.
*/
public class CarteiraService {
    private final CarteiraRepository repositorio;

    public CarteiraService(CarteiraRepository repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio nao pode ser nulo");
    }

    /*
      Se o ativo ja esta na carteira, soma a quantidade e recalcula o preco medio.
      Se for novo, pede a criacao para a AtivoFactory e salva no repositorio.
    */
    public void comprar(TipoAtivo tipo, String ticker, String nome, double quantidade, double preco)
            throws TickerInvalidoException {
        AtivoFinanceiro ativo = repositorio.buscar(ticker);
        if (ativo == null) {
            repositorio.salvar(AtivoFactory.criarAtivo(tipo, ticker, nome, quantidade, preco));
            return;
        }
        if (!ehDoTipo(ativo, tipo)) {
            throw new IllegalArgumentException(ativo.getTicker() + " ja esta na carteira com outro tipo de ativo.");
        }
        ativo.comprar(quantidade, preco);
    }

    // Quando a venda zera a posicao, o ativo sai da carteira (e o monitor para de consultar a cotacao dele)
    public void vender(String ticker, double quantidade)
            throws AtivoNaoEncontradoException, SaldoInsuficienteException {
        AtivoFinanceiro ativo = buscarObrigatorio(ticker);
        ativo.vender(quantidade);
        if (ativo.getQuantidade() == 0.0) {
            repositorio.remover(ativo.getTicker());
        }
    }

    // Aplica em uma renda fixa. Se ja existe uma aplicacao com esse nome, adiciona o valor a ela
    public void investirRendaFixa(String nome, double valor, double taxaAnual, LocalDate vencimento) {
        AtivoFinanceiro ativo = repositorio.buscar(nome);
        if (ativo == null) {
            repositorio.salvar(AtivoFactory.criarRendaFixa(nome, valor, taxaAnual, vencimento));
            return;
        }
        if (!(ativo instanceof RendaFixa rendaFixa)) {
            throw new IllegalArgumentException(ativo.getTicker() + " ja existe na carteira e nao eh renda fixa.");
        }
        rendaFixa.investir(valor);
    }

    /*
      Resgata um valor em reais e devolve uma mensagem para a tela.
      Avisa quando o resgate acontece antes do vencimento, com a aliquota de IR atual.
    */
    public String resgatarRendaFixa(String nome, double valor)
            throws AtivoNaoEncontradoException, SaldoInsuficienteException {
        AtivoFinanceiro ativo = buscarObrigatorio(nome);
        if (!(ativo instanceof RendaFixa rendaFixa)) {
            throw new IllegalArgumentException(ativo.getTicker() + " nao eh uma renda fixa.");
        }

        boolean antesDoVencimento = rendaFixa.isAntesDoVencimento();
        double aliquota = rendaFixa.calcularAliquotaIR();
        rendaFixa.resgatar(valor);
        if (rendaFixa.getQuantidade() == 0.0) {
            repositorio.remover(rendaFixa.getTicker());
        }

        Locale ptBr = Locale.forLanguageTag("pt-BR");
        if (antesDoVencimento) {
            return String.format(ptBr, "Resgate de R$ %.2f feito antes do vencimento. Aliquota de IR atual: %.1f%%.",
                    valor, aliquota * 100);
        }
        return String.format(ptBr, "Resgate de R$ %.2f feito.", valor);
    }

    // Soma do valor atual de todos os ativos da carteira
    public double calcularPatrimonioTotal() {
        double total = 0.0;
        for (AtivoFinanceiro ativo : repositorio.listarTodos()) {
            total += ativo.calcularValorAtual();
        }
        return total;
    }

    // Soma do lucro ou prejuizo ainda nao realizado de todos os ativos
    public double calcularLucroPrejuizoTotal() {
        double total = 0.0;
        for (AtivoFinanceiro ativo : repositorio.listarTodos()) {
            total += ativo.calcularLucroPrejuizo();
        }
        return total;
    }

    // Renda fixa nunca passa por comprar: ela usa investirRendaFixa
    private static boolean ehDoTipo(AtivoFinanceiro ativo, TipoAtivo tipo) {
        return (tipo == TipoAtivo.ACAO && ativo instanceof Acao)
                || (tipo == TipoAtivo.CRIPTOMOEDA && ativo instanceof Criptomoeda);
    }

    private AtivoFinanceiro buscarObrigatorio(String ticker) throws AtivoNaoEncontradoException {
        AtivoFinanceiro ativo = repositorio.buscar(ticker);
        if (ativo == null) {
            throw new AtivoNaoEncontradoException("Ativo nao encontrado na carteira: " + ticker + ".");
        }
        return ativo;
    }
}
