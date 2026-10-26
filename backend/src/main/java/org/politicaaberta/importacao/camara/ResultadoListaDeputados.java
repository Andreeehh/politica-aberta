package org.politicaaberta.importacao.camara;

import java.util.List;

/**
 * Deputados das paginas nao vazias e o total visto na primeira
 * pagina nao vazia que trouxe {@code X-Total-Count} utilizavel.
 * {@code totalConhecido} nulo significa que a API nao informou
 * um total confiavel. Zero vindo de pagina vazia nao entra aqui.
 */
public record ResultadoListaDeputados(List<DeputadoEmLista> deputados, Long totalConhecido) {

    public ResultadoListaDeputados {
        deputados = List.copyOf(deputados);
    }
}
