package org.politicaaberta.web;

public final class DeputadoNaoEncontrado extends RuntimeException {

    public DeputadoNaoEncontrado(long id) {
        super("Deputado " + id + " nao encontrado.");
    }
}
