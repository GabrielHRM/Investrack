package br.ufrn.imd.investtrack.exception;

// Lancada quando uma operacao pede um ativo que nao esta na carteira
public class AtivoNaoEncontradoException extends Exception {
    private static final long serialVersionUID = 1L;

    public AtivoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
