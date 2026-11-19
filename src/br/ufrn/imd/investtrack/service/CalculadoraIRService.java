package br.ufrn.imd.investtrack.service;

import br.ufrn.imd.investtrack.interfaces.Tributavel;
import br.ufrn.imd.investtrack.model.AtivoFinanceiro;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/*
 Calcula o imposto estimado de uma lista de itens tributaveis.
 Uso polimorfico: cada item calcula o proprio imposto (acao e cripto 15%, renda fixa pela tabela regressiva),
 e esta classe so percorre a lista sem saber o tipo de cada um.
*/
public class CalculadoraIRService {

    public double calcularImpostoTotal(List<Tributavel> itens) {
        Objects.requireNonNull(itens, "lista de itens nao pode ser nula");
        double total = 0.0;
        for (Tributavel item : itens) {
            total += item.calcularImposto();
        }
        return total;
    }

    // Monta o texto do relatorio: uma linha por item e o total no final
    public String gerarRelatorio(List<Tributavel> itens) {
        Objects.requireNonNull(itens, "lista de itens nao pode ser nula");
        Locale ptBr = Locale.forLanguageTag("pt-BR");
        StringBuilder relatorio = new StringBuilder("Relatorio de IR estimado\n");

        for (Tributavel item : itens) {
            relatorio.append(String.format(ptBr, "%s: R$ %.2f%n", descrever(item), item.calcularImposto()));
        }
        relatorio.append(String.format(ptBr, "Total: R$ %.2f", calcularImpostoTotal(itens)));
        return relatorio.toString();
    }

    // Usa o ticker quando o item eh um ativo; senao, o nome da classe
    private static String descrever(Tributavel item) {
        if (item instanceof AtivoFinanceiro ativo) {
            return ativo.getTicker();
        }
        return item.getClass().getSimpleName();
    }
}
