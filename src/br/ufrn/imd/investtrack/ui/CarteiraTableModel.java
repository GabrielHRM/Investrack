package br.ufrn.imd.investtrack.ui;

import br.ufrn.imd.investtrack.model.Acao;
import br.ufrn.imd.investtrack.model.AtivoFinanceiro;
import br.ufrn.imd.investtrack.model.Criptomoeda;
import br.ufrn.imd.investtrack.model.RendaFixa;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Adapta os ativos da carteira para JTable. Usa AbstractTableModel do Swing
 * e List/Comparator de java.util para armazenar e ordenar as linhas exibidas.
 */
public final class CarteiraTableModel extends AbstractTableModel {
	private static final String[] COLUNAS = {
			"Investimento", "Ticker", "Tipo", "Quantidade", "Preço médio",
			"Cotação", "Valor atual", "Lucro / prejuízo"
	};

	private List<AtivoFinanceiro> ativos = List.of();

	/** Copia e ordena os ativos por ticker e avisa JTable que os dados mudaram. */
	public void atualizar(List<AtivoFinanceiro> novosAtivos) {
		List<AtivoFinanceiro> copia = new ArrayList<>(novosAtivos);
		copia.sort(Comparator.comparing(AtivoFinanceiro::getTicker));
		ativos = List.copyOf(copia);
		fireTableDataChanged();
	}

	/** Devolve o ativo associado a uma linha do modelo. */
	public AtivoFinanceiro getAtivoAt(int linha) {
		return ativos.get(linha);
	}

	/** Informa a JTable quantas linhas de ativos estão disponíveis. */
	@Override
	public int getRowCount() {
		return ativos.size();
	}

	/** Informa a JTable quantas colunas a tabela possui. */
	@Override
	public int getColumnCount() {
		return COLUNAS.length;
	}

	/** Fornece o título da coluna solicitado pela JTable. */
	@Override
	public String getColumnName(int coluna) {
		return COLUNAS[coluna];
	}

	/** Marca colunas numéricas como Double para formatação e ordenação numérica. */
	@Override
	public Class<?> getColumnClass(int coluna) {
		return coluna >= 3 ? Double.class : String.class;
	}

	/** Mapeia cada coluna e calcula valor atual e lucro/prejuízo com os dados do ativo. */
	@Override
	public Object getValueAt(int linha, int coluna) {
		AtivoFinanceiro ativo = ativos.get(linha);
		return switch (coluna) {
			case 0 -> ativo.getNome();
			case 1 -> ativo.getTicker();
			case 2 -> ativo instanceof Acao ? "Ação"
					: ativo instanceof Criptomoeda ? "Criptomoeda"
					: ativo instanceof RendaFixa ? "Renda fixa" : "Outro";
			case 3 -> ativo.getQuantidade();
			case 4 -> ativo.getPrecoMedio();
			case 5 -> ativo.getCotacaoAtual();
			case 6 -> ativo.calcularValorAtual();
			case 7 -> ativo.calcularLucroPrejuizo();
			default -> throw new IndexOutOfBoundsException("Coluna invalida: " + coluna);
		};
	}
}