package br.ufrn.imd.investtrack.repository;

import br.ufrn.imd.investtrack.interfaces.Tributavel;
import br.ufrn.imd.investtrack.model.AtivoFinanceiro;
import br.ufrn.imd.investtrack.model.RendaFixa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/*
 Guarda os ativos da carteira em memoria. Segue o padrao Singleton: existe uma unica carteira
 no programa, compartilhada pela tela e pela thread do monitor de mercado.
 O ConcurrentHashMap permite que as duas threads leiam e alterem o mapa ao mesmo tempo sem erro.
*/
public class CarteiraRepository {
    private static CarteiraRepository instancia;

    private final ConcurrentHashMap<String, AtivoFinanceiro> ativos; // chave: ticker em maiusculas

    // Construtor privado: ninguem de fora consegue dar new, so pelo getInstancia()
    private CarteiraRepository() {
        this.ativos = new ConcurrentHashMap<>();
    }

    /*
      synchronized para que duas threads chamando ao mesmo tempo pela primeira vez
      nao criem duas instancias diferentes.
    */
    public static synchronized CarteiraRepository getInstancia() {
        if (instancia == null) {
            instancia = new CarteiraRepository();
        }
        return instancia;
    }

    // Salva o ativo. Se ja existir um ativo com o mesmo ticker, ele eh substituido
    public void salvar(AtivoFinanceiro ativo) {
        Objects.requireNonNull(ativo, "ativo nao pode ser nulo");
        ativos.put(ativo.getTicker(), ativo);
    }

    // Retorna o ativo do ticker informado, ou null se ele nao estiver na carteira
    public AtivoFinanceiro buscar(String ticker) {
        if (ticker == null) {
            return null;
        }
        return ativos.get(normalizar(ticker));
    }

    public void remover(String ticker) {
        ativos.remove(normalizar(ticker));
    }

    public List<AtivoFinanceiro> listarTodos() {
        return new ArrayList<>(ativos.values());
    }

    // Tickers que o monitor consulta nas APIs. Renda fixa fica de fora porque nao tem cotacao de mercado
    public Set<String> listarTickersMonitorados() {
        Set<String> tickers = new HashSet<>();
        for (AtivoFinanceiro ativo : ativos.values()) {
            if (!(ativo instanceof RendaFixa)) {
                tickers.add(ativo.getTicker());
            }
        }
        return tickers;
    }

    // Ativos que podem gerar imposto, usados pela CalculadoraIRService
    public List<Tributavel> listarTributaveis() {
        List<Tributavel> tributaveis = new ArrayList<>();
        for (AtivoFinanceiro ativo : ativos.values()) {
            if (ativo instanceof Tributavel tributavel) {
                tributaveis.add(tributavel);
            }
        }
        return tributaveis;
    }

    // Mesma regra do AtivoFinanceiro: o ticker eh guardado sem espacos e em maiusculas
    private static String normalizar(String ticker) {
        return Objects.requireNonNull(ticker, "ticker nao pode ser nulo").trim().toUpperCase(Locale.ROOT);
    }
}
