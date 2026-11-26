package br.ufrn.imd.investtrack.ui;

import br.ufrn.imd.investtrack.api.ProvedorApiReal;
import br.ufrn.imd.investtrack.api.ProvedorSimulado;
import br.ufrn.imd.investtrack.interfaces.ProvedorCotacao;
import br.ufrn.imd.investtrack.repository.CarteiraRepository;
import br.ufrn.imd.investtrack.service.CarteiraService;
import br.ufrn.imd.investtrack.service.MonitorMercadoService;

import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/*
Ponto de entrada. SwingUtilities inicia a interface na thread de eventos do Swing;
UIManager configura o tema, e Color do AWT define a paleta escura.
*/
public final class InvestTrackApp {
	// Classe utilitaria; a aplicação inicia pelo método main.
	private InvestTrackApp() { }

	// Define cores padrão usadas pela janela e pelos diálogos Swing.
	private static void configurarTema() {
		Color fundo = new Color(12, 12, 12);
		Color superficie = new Color(25, 25, 25);
		Color texto = new Color(245, 245, 245);
		Color selecao = new Color(45, 65, 90);
		Color botao = new Color(96, 96, 96);

		UIManager.put("Panel.background", fundo);
		UIManager.put("OptionPane.background", fundo);
		UIManager.put("OptionPane.messageForeground", texto);
		UIManager.put("Label.foreground", texto);
		UIManager.put("TextField.background", superficie);
		UIManager.put("TextField.foreground", texto);
		UIManager.put("TextField.caretForeground", texto);
		UIManager.put("TextField.selectionBackground", selecao);
		UIManager.put("TextField.selectionForeground", texto);
		UIManager.put("ComboBox.background", superficie);
		UIManager.put("ComboBox.foreground", Color.WHITE);
		UIManager.put("ComboBox.selectionBackground", selecao);
		UIManager.put("ComboBox.selectionForeground", texto);
		UIManager.put("List.background", superficie);
		UIManager.put("List.foreground", Color.WHITE);
		UIManager.put("Button.background", botao);
		UIManager.put("Button.foreground", Color.WHITE);
		UIManager.put("Button.select", selecao);
		UIManager.put("TableHeader.background", botao);
		UIManager.put("TableHeader.foreground", Color.WHITE);
	}

	// Aplica o tema, cria o repositório e o serviço, abre o dashboard e liga o monitor de cotações.
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception ignored) {
				// Mantém o tema padrão quando o Look and Feel do sistema não estiver disponível.
			}
			configurarTema();
			CarteiraRepository repositorio = CarteiraRepository.getInstancia();
			MonitorMercadoService monitor = new MonitorMercadoService(escolherProvedor(args, repositorio), repositorio);
			DashboardFrame janela = new DashboardFrame(new CarteiraService(repositorio), repositorio);
			// O monitor começa junto com a janela e para quando ela é fechada.
			janela.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent evento) {
					monitor.parar();
				}
			});
			janela.setVisible(true);
			monitor.iniciar();
		});
	}

	// Por padrão usa a internet; com o argumento --simulado usa preços simulados (sem internet).
	private static ProvedorCotacao escolherProvedor(String[] args, CarteiraRepository repositorio) {
		if (args.length > 0 && args[0].equals("--simulado")) {
			return new ProvedorSimulado(repositorio, System.currentTimeMillis());
		}
		return new ProvedorApiReal();
	}
}
