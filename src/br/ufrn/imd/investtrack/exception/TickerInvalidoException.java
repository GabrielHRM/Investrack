package br.ufrn.imd.investtrack.exception;

// Lancada quando o ticker informado nao segue o formato esperado para o tipo de ativo
public class TickerInvalidoException extends Exception {
    private static final long serialVersionUID = 1L;

    public TickerInvalidoException(String mensagem) {
        super(mensagem);
    }
}
