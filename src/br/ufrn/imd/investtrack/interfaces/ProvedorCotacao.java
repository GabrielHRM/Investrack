package br.ufrn.imd.investtrack.interfaces;

import br.ufrn.imd.investtrack.exception.CotacaoIndisponivelException;

/*
 Fonte de cotacoes usada pelo MonitorMercadoService (padrao Strategy).
 O monitor so conhece esta interface, entao da para trocar a fonte (API real ou simulada)
 sem mudar o codigo do monitor.
*/
public interface ProvedorCotacao {
    // Devolve o preco atual de 1 unidade do ativo, em reais
    double buscarCotacaoAtual(String ticker) throws CotacaoIndisponivelException;
}
