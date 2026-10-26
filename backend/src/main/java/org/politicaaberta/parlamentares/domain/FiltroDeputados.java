package org.politicaaberta.parlamentares.domain;

/**
 * Filtros opcionais da consulta. Texto em branco vira ausencia de filtro.
 */
public record FiltroDeputados(String siglaUf, String siglaPartido) {

    public FiltroDeputados {
        siglaUf = limpar(siglaUf);
        siglaPartido = limpar(siglaPartido);
    }

    public static FiltroDeputados nenhum() {
        return new FiltroDeputados(null, null);
    }

    private static String limpar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
