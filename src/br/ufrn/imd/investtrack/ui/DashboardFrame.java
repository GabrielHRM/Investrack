package br.ufrn.imd.investtrack.ui;

import br.ufrn.imd.investtrack.exception.AtivoNaoEncontradoException;
import br.ufrn.imd.investtrack.exception.SaldoInsuficienteException;
import br.ufrn.imd.investtrack.exception.TickerInvalidoException;
import br.ufrn.imd.investtrack.model.AtivoFinanceiro;
import br.ufrn.imd.investtrack.model.RendaFixa;
import br.ufrn.imd.investtrack.model.TipoAtivo;
import br.ufrn.imd.investtrack.repository.CarteiraRepository;
import br.ufrn.imd.investtrack.service.CarteiraService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/*
Tela principal da carteira. Usa Swing para janelas, tabela, botoes, caixas de dialogo e timer;
AWT para cores e layouts; NumberFormat e Locale para valores em pt-BR.
CarteiraService executa compras e vendas, e CarteiraRepository fornece os ativos exibidos.
*/
public final class DashboardFrame extends JFrame {
	private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
	private static final Color FUNDO = new Color(12, 12, 12);
	private static final Color SUPERFICIE = new Color(25, 25, 25);
	private static final Color BORDA = new Color(56, 56, 56);
	private static final Color TEXTO = new Color(245, 245, 245);
	private static final Color CINZA = new Color(175, 175, 175);
	private static final Color VERDE = new Color(105, 210, 155);
	private static final Color VERMELHO = new Color(255, 125, 125);
	private static final Color SELECAO = new Color(45, 65, 90);
	private static final Color BOTAO = new Color(96, 96, 96);

	private final CarteiraService service;
	private final CarteiraRepository repositorio;
	private final CarteiraTableModel modelo = new CarteiraTableModel();
	private final JTable tabela = new JTable(modelo);
	private final JButton vender = new JButton("Vender");
	private final JLabel patrimonio = new JLabel();
	private final JLabel lucro = new JLabel();
	private final JLabel status = new JLabel();

	// Configura a janela, monta a tela, carrega a carteira e agenda a atualização visual.
	public DashboardFrame(CarteiraService service, CarteiraRepository repositorio) {
		super("InvestTrack");
		this.service = service;
		this.repositorio = repositorio;
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(1120, 700);
		setMinimumSize(new Dimension(950, 590));
		setLocationRelativeTo(null);
		montarTela();
		atualizarCarteira();
		new Timer(2000, e -> atualizarCarteira()).start();
	}

	// Monta o cabeçalho, os resumos, a tabela de ativos e os botões de operação.
	private void montarTela() {
		JPanel raiz = new JPanel(new BorderLayout(0, 18));
		raiz.setBackground(FUNDO);
		raiz.setBorder(new EmptyBorder(26, 30, 22, 30));
		setContentPane(raiz);

		JPanel cabecalho = new JPanel(new BorderLayout(0, 14));
		cabecalho.setOpaque(false);
		JLabel titulo = new JLabel("InvestTrack");
		titulo.setFont(new Font("SansSerif", Font.BOLD, 26));
		titulo.setForeground(TEXTO);
		JLabel subtitulo = new JLabel("Acompanhe sua carteira de investimentos");
		subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 14));
		subtitulo.setForeground(CINZA);
		JPanel titulos = new JPanel(new GridLayout(2, 1, 0, 3));
		titulos.setOpaque(false);
		titulos.add(titulo);
		titulos.add(subtitulo);
		cabecalho.add(titulos, BorderLayout.NORTH);

		JPanel cards = new JPanel(new GridLayout(1, 2, 12, 0));
		cards.setOpaque(false);
		cards.add(cartao("Patrimônio atual", patrimonio));
		cards.add(cartao("Lucro / prejuízo", lucro));
		cabecalho.add(cards, BorderLayout.CENTER);
		raiz.add(cabecalho, BorderLayout.NORTH);

		configurarTabela();
		JPanel conteudo = new JPanel(new BorderLayout(0, 12));
		conteudo.setOpaque(false);
		JPanel barra = new JPanel(new BorderLayout());
		barra.setOpaque(false);
		JLabel investimentos = new JLabel("Seus investimentos");
		investimentos.setFont(new Font("SansSerif", Font.BOLD, 17));
		investimentos.setForeground(TEXTO);
		barra.add(investimentos, BorderLayout.WEST);
		JButton comprar = new JButton("Comprar ativo");
		configurarBotao(comprar);
		comprar.addActionListener(e -> comprarAtivo());
		barra.add(comprar, BorderLayout.EAST);
		conteudo.add(barra, BorderLayout.NORTH);
		JScrollPane rolagem = new JScrollPane(tabela);
		rolagem.setBorder(BorderFactory.createLineBorder(BORDA));
		rolagem.getViewport().setBackground(SUPERFICIE);
		conteudo.add(rolagem, BorderLayout.CENTER);
		raiz.add(conteudo, BorderLayout.CENTER);

		JPanel rodape = new JPanel(new BorderLayout());
		rodape.setOpaque(false);
		status.setFont(new Font("SansSerif", Font.PLAIN, 12));
		status.setForeground(CINZA);
		rodape.add(status, BorderLayout.WEST);
		configurarBotao(vender);
		vender.setEnabled(false);
		vender.addActionListener(e -> venderSelecionado());
		rodape.add(vender, BorderLayout.EAST);
		raiz.add(rodape, BorderLayout.SOUTH);
	}

	// Cria um cartão para um indicador, como patrimônio ou lucro.
	private JPanel cartao(String titulo, JLabel valor) {
		JPanel painel = new JPanel(new BorderLayout(0, 7));
		painel.setBackground(SUPERFICIE);
		painel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(BORDA),
				new EmptyBorder(12, 16, 12, 16)));
		JLabel rotulo = new JLabel(titulo);
		rotulo.setForeground(CINZA);
		rotulo.setFont(new Font("SansSerif", Font.PLAIN, 13));
		valor.setForeground(TEXTO);
		valor.setFont(new Font("SansSerif", Font.BOLD, 21));
		painel.add(rotulo, BorderLayout.NORTH);
		painel.add(valor, BorderLayout.CENTER);
		return painel;
	}

	// Configura colunas, ordenação, seleção, cores e formatação das células.
	private void configurarTabela() {
		tabela.setFillsViewportHeight(true);
		tabela.setRowHeight(37);
		tabela.setShowVerticalLines(false);
		tabela.setGridColor(BORDA);
		tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tabela.setAutoCreateRowSorter(true);
		tabela.setFont(new Font("SansSerif", Font.PLAIN, 13));
		tabela.setForeground(TEXTO);
		tabela.setBackground(SUPERFICIE);
		tabela.setSelectionBackground(SELECAO);
		tabela.setSelectionForeground(TEXTO);
		JTableHeader cabecalho = tabela.getTableHeader();
		cabecalho.setUI(new javax.swing.plaf.basic.BasicTableHeaderUI());
		cabecalho.setFont(new Font("SansSerif", Font.BOLD, 12));
		cabecalho.setForeground(Color.WHITE);
		cabecalho.setBackground(BOTAO);
		cabecalho.setOpaque(true);
		cabecalho.setDefaultRenderer(new CabecalhoRenderer());
		cabecalho.setPreferredSize(new Dimension(0, 38));
		cabecalho.setReorderingAllowed(false);
		for (int i = 3; i < modelo.getColumnCount(); i++) {
			tabela.getColumnModel().getColumn(i).setCellRenderer(new NumeroRenderer(i >= 4));
		}
		tabela.getColumnModel().getColumn(7).setCellRenderer(new LucroRenderer());
		tabela.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) atualizarBotaoVender();
		});
	}

	// Aplica uma aparencia uniforme e cores explicitas para o texto dos botoes.
	private void configurarBotao(JButton botao) {
		botao.setUI(new javax.swing.plaf.basic.BasicButtonUI());
		botao.setBackground(BOTAO);
		botao.setForeground(Color.WHITE);
		botao.setOpaque(true);
		botao.setContentAreaFilled(true);
		botao.setBorderPainted(true);
		botao.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(BORDA),
				new EmptyBorder(8, 14, 8, 14)));
		botao.setFocusPainted(false);
	}
	// Abre a confirmacao de compra e devolve a opcao escolhida pelo usuario.
	private int confirmarCompra(JPanel formulario) {
		JOptionPane painel = new JOptionPane(formulario, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		Object resposta = exibirDialogo(painel, "Comprar ativo");
		if (resposta instanceof Integer) {
			return ((Integer) resposta).intValue();
		}
		return JOptionPane.CLOSED_OPTION;
	}

	// Solicita texto em uma caixa estilizada e devolve null se o usuario cancelar.
	private String solicitarEntrada(String rotulo, String titulo) {
		JTextField campo = new JTextField(16);
		JPanel conteudo = new JPanel(new BorderLayout(8, 0));
		conteudo.add(new JLabel(rotulo), BorderLayout.WEST);
		conteudo.add(campo, BorderLayout.CENTER);
		JOptionPane painel = new JOptionPane(conteudo, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		Object resposta = exibirDialogo(painel, titulo);
		if (resposta instanceof Integer && ((Integer) resposta).intValue() == JOptionPane.OK_OPTION) {
			return campo.getText();
		}
		return null;
	}

	// Exibe mensagens com o mesmo estilo dos botoes de compra e venda.
	private void mostrarMensagem(String texto, String titulo, int tipo) {
		JOptionPane painel = new JOptionPane(texto, tipo, JOptionPane.DEFAULT_OPTION);
		exibirDialogo(painel, titulo);
	}

	// Mostra um JOptionPane modal e estiliza seus botoes antes de abrir a janela.
	private Object exibirDialogo(JOptionPane painel, String titulo) {
		JDialog dialogo = painel.createDialog(this, titulo);
		estilizarBotoes(dialogo.getContentPane());
		dialogo.setModal(true);
		dialogo.setVisible(true);
		dialogo.dispose();
		return painel.getValue();
	}

	// Percorre os componentes do dialogo e aplica o tema a cada botao.
	private void estilizarBotoes(Container container) {
		for (Component componente : container.getComponents()) {
			if (componente instanceof JButton botao) {
				configurarBotao(botao);
			} else if (componente instanceof Container interno) {
				estilizarBotoes(interno);
			}
		}
	}
	// Configura o seletor de tipo com fundo preto e texto branco.
	private void configurarComboBox(JComboBox<String> combo) {
		combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI());
		combo.setBackground(FUNDO);
		combo.setForeground(Color.WHITE);
		combo.setOpaque(true);
		combo.setBorder(BorderFactory.createLineBorder(BORDA));
		combo.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
														  boolean selecionado, boolean foco) {
				JLabel rotulo = (JLabel) super.getListCellRendererComponent(
						lista, valor, indice, selecionado, foco);
				rotulo.setOpaque(true);
				rotulo.setBackground(selecionado ? SELECAO : FUNDO);
				rotulo.setForeground(Color.WHITE);
				return rotulo;
			}
		});
	}
	// Lê os dados do formulário, valida os valores e registra a compra pelo serviço.
	private void comprarAtivo() {
		JComboBox<String> tipo = new JComboBox<>(new String[]{"Ação", "Criptomoeda"});
		configurarComboBox(tipo);
		JTextField ticker = new JTextField(16);
		JTextField nome = new JTextField(16);
		JTextField quantidade = new JTextField(16);
		JTextField preco = new JTextField(16);
		JPanel formulario = new JPanel(new GridLayout(0, 2, 8, 8));
		formulario.add(new JLabel("Tipo:")); formulario.add(tipo);
		formulario.add(new JLabel("Ticker (PETR4 ou BTCBRL):")); formulario.add(ticker);
		formulario.add(new JLabel("Nome:")); formulario.add(nome);
		formulario.add(new JLabel("Quantidade:")); formulario.add(quantidade);
		formulario.add(new JLabel("Preço por unidade (R$):")); formulario.add(preco);

		while (true) {
			if (confirmarCompra(formulario) != JOptionPane.OK_OPTION) return;
			try {
				if (ticker.getText().isBlank() || nome.getText().isBlank()) {
					throw new IllegalArgumentException("Preencha o ticker e o nome do ativo.");
				}
				TipoAtivo tipoAtivo = tipo.getSelectedIndex() == 0 ? TipoAtivo.ACAO : TipoAtivo.CRIPTOMOEDA;
				service.comprar(tipoAtivo, ticker.getText().trim(), nome.getText().trim(),
						lerNumero(quantidade.getText(), "quantidade"), lerNumero(preco.getText(), "preço"));
				atualizarCarteira();
				mostrarMensagem("Compra registrada com sucesso.", "Compra", JOptionPane.INFORMATION_MESSAGE);
				return;
			} catch (TickerInvalidoException | IllegalArgumentException erro) {
				mostrarMensagem(erro.getMessage(), "Confira os dados", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	// Lê a quantidade do item selecionado e registra a venda pelo serviço da carteira.
	private void venderSelecionado() {
		int linha = tabela.getSelectedRow();
		if (linha < 0) return;
		AtivoFinanceiro ativo = modelo.getAtivoAt(tabela.convertRowIndexToModel(linha));
		if (ativo instanceof RendaFixa) return;
		String texto = solicitarEntrada("Quantidade de " + ativo.getTicker() + " para vender:", "Vender ativo");
		if (texto == null) return;
		try {
			service.vender(ativo.getTicker(), lerNumero(texto, "quantidade"));
			atualizarCarteira();
			mostrarMensagem("Venda registrada com sucesso.", "Venda", JOptionPane.INFORMATION_MESSAGE);
		} catch (AtivoNaoEncontradoException | SaldoInsuficienteException | IllegalArgumentException erro) {
			mostrarMensagem(erro.getMessage(), "Não foi possível vender", JOptionPane.ERROR_MESSAGE);
		}
	}

	// Atualiza linhas e totais, mantendo a seleção quando o ativo ainda existe.
	private void atualizarCarteira() {
		String selecionado = tickerSelecionado();
		List<AtivoFinanceiro> ativos = repositorio.listarTodos();
		modelo.atualizar(ativos);
		if (selecionado != null) {
			for (int i = 0; i < modelo.getRowCount(); i++) {
				if (modelo.getAtivoAt(i).getTicker().equals(selecionado)) {
					int view = tabela.convertRowIndexToView(i);
					if (view >= 0) tabela.setRowSelectionInterval(view, view);
					break;
				}
			}
		}
		double total = 0.0, resultado = 0.0;
		for (AtivoFinanceiro ativo : ativos) {
			total += ativo.calcularValorAtual();
			resultado += ativo.calcularLucroPrejuizo();
		}
		patrimonio.setText(NumberFormat.getCurrencyInstance(PT_BR).format(total));
		lucro.setText(NumberFormat.getCurrencyInstance(PT_BR).format(resultado));
		lucro.setForeground(resultado >= 0 ? VERDE : VERMELHO);
		status.setText(ativos.isEmpty() ? "Carteira vazia. Use Comprar ativo para começar."
				: ativos.size() + (ativos.size() == 1 ? " investimento" : " investimentos"));
		atualizarBotaoVender();
	}

	// Retorna o ticker selecionado na tabela ou null se não houver seleção.
	private String tickerSelecionado() {
		int linha = tabela.getSelectedRow();
		return linha < 0 ? null : modelo.getAtivoAt(tabela.convertRowIndexToModel(linha)).getTicker();
	}

	// Habilita venda para ativos negociaveis; renda fixa usa a operação de resgate.
	private void atualizarBotaoVender() {
		int linha = tabela.getSelectedRow();
		vender.setEnabled(linha >= 0
				&& !(modelo.getAtivoAt(tabela.convertRowIndexToModel(linha)) instanceof RendaFixa));
	}

	// Aceita ponto ou vírgula decimal e rejeita números inválidos ou não positivos.
	private static double lerNumero(String texto, String campo) {
		String valor = texto == null ? "" : texto.trim().replace(" ", "");
		if (valor.contains(",")) valor = valor.replace(".", "").replace(',', '.');
		try {
			double numero = Double.parseDouble(valor);
			if (!Double.isFinite(numero) || numero <= 0) throw new NumberFormatException();
			return numero;
		} catch (NumberFormatException erro) {
			throw new IllegalArgumentException("Informe " + campo + " como número maior que zero.");
		}
	}

	// Renderiza cada titulo em branco sobre o mesmo cinza dos botoes.
	private static final class CabecalhoRenderer extends DefaultTableCellRenderer {
		private CabecalhoRenderer() {
			setOpaque(true);
			setHorizontalAlignment(SwingConstants.CENTER);
		}

		@Override
		public Component getTableCellRendererComponent(JTable tabela, Object valor, boolean selecionado,
													   boolean foco, int linha, int coluna) {
			JLabel rotulo = (JLabel) super.getTableCellRendererComponent(tabela, valor, false, foco, linha, coluna);
			rotulo.setBackground(BOTAO);
			rotulo.setForeground(Color.WHITE);
			rotulo.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, BORDA));
			rotulo.setOpaque(true);
			return rotulo;
		}
	}
	// Formata quantidades e valores monetários exibidos nas células numéricas.
	private static class NumeroRenderer extends DefaultTableCellRenderer {
		private final boolean moeda;
		// Escolhe o alinhamento e se o valor será apresentado como moeda.
		NumeroRenderer(boolean moeda) {
			this.moeda = moeda;
			setHorizontalAlignment(SwingConstants.RIGHT);
		}
		// Converte números para formato brasileiro de quantidade ou moeda.
		@Override
		protected void setValue(Object valor) {
			if (!(valor instanceof Number n)) { setText(""); return; }
			if (moeda) setText(NumberFormat.getCurrencyInstance(PT_BR).format(n.doubleValue()));
			else {
				NumberFormat formato = NumberFormat.getNumberInstance(PT_BR);
				formato.setMaximumFractionDigits(8);
				setText(formato.format(n.doubleValue()));
			}
		}
	}

	// Coloriza lucro e prejuízo para facilitar a leitura do resultado.
	private static final class LucroRenderer extends NumeroRenderer {
		// Inicializa o renderizador usando formato monetário.
		LucroRenderer() { super(true); }
		// Aplica verde para lucro e vermelho para prejuízo sem alterar a cor da seleção.
		@Override
		public Component getTableCellRendererComponent(JTable tabela, Object valor, boolean selecionado,
													   boolean foco, int linha, int coluna) {
			Component c = super.getTableCellRendererComponent(tabela, valor, selecionado, foco, linha, coluna);
			if (!selecionado && valor instanceof Number n) c.setForeground(n.doubleValue() >= 0 ? VERDE : VERMELHO);
			return c;
		}
	}
}
