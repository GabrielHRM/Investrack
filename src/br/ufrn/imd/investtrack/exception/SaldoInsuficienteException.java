package br.ufrn.imd.investtrack.exception;

// Lancada quando uma venda ou um resgate excede a posicao disponivel
public class SaldoInsuficienteException extends Exception {
    private static final long serialVersionUID = 1L;

    public SaldoInsuficienteException(String mensagem) {
        super(mensagem);
    }
}