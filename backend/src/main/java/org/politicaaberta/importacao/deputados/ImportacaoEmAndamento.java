package org.politicaaberta.importacao.deputados;

public final class ImportacaoEmAndamento extends RuntimeException {

    public ImportacaoEmAndamento(String tipo) {
        super("ja existe importacao em execucao: " + tipo);
    }
}
