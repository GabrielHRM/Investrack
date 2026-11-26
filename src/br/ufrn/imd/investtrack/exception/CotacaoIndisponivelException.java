package br.ufrn.imd.investtrack.exception;

// Lancada quando nao foi possivel obter a cotacao de um ativo (sem internet, API fora do ar, resposta invalida)
public class CotacaoIndisponivelException extends Exception {
    private static final long serialVersionUID = 1L;

    public CotacaoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
