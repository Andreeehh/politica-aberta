package org.politicaaberta.web;

public final class RequisicaoInvalida extends RuntimeException {

    public RequisicaoInvalida(String mensagem) {
        super(mensagem);
    }
}
