package br.ufrn.imd.investtrack.interfaces;

/*
 Representa um item cuja posicao atual permite estimar imposto.
 O resultado eh expresso em reais.
*/
public interface Tributavel {
    double calcularImposto();
}