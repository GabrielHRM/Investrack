package br.ufrn.imd.investtrack.api;

import br.ufrn.imd.investtrack.exception.CotacaoIndisponivelException;
import br.ufrn.imd.investtrack.interfaces.ProvedorCotacao;
import br.ufrn.imd.investtrack.model.AtivoFinanceiro;
import br.ufrn.imd.investtrack.repository.CarteiraRepository;

import java.util.Objects;
import java.util.Random;

/*
 Provedor de cotacoes sem internet, para testar o sistema e como plano B na apresentacao.
 Parte da cotacao atual do ativo na carteira e sorteia uma variacao de ate 2% para cima ou para baixo,
 entao o preco anda aos poucos, como no mercado de verdade.
*/
public class ProvedorSimulado implements ProvedorCotacao {
    private static final double VARIACAO_MAXIMA = 0.02; // 2%

    private final CarteiraRepository repositorio;
    private final Random aleatorio;

    // A semente deixa a sequencia de precos previsivel nos testes (mesma semente, mesmos precos)
    public ProvedorSimulado(CarteiraRepository repositorio, long semente) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio nao pode ser nulo");
        this.aleatorio = new Random(semente);
    }

    @Override
    public double buscarCotacaoAtual(String ticker) throws CotacaoIndisponivelException {
        AtivoFinanceiro ativo = repositorio.buscar(ticker);
        if (ativo == null) {
            throw new CotacaoIndisponivelException("Sem preco de referencia para simular " + ticker + ".");
        }

        // nextDouble() vai de 0 a 1; vezes 2 menos 1 vai de -1 a 1
        double variacao = (aleatorio.nextDouble() * 2 - 1) * VARIACAO_MAXIMA;
        return ativo.getCotacaoAtual() * (1 + variacao);
    }
}
