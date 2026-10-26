package org.politicaaberta.importacao.camara;

/**
 * Campos da lista {@code GET /deputados}. Nao inclui CPF:
 * esse campo so aparece no detalhe e nao e persistido aqui.
 */
public record DeputadoEmLista(
        long id,
        String uri,
        String nome,
        String siglaPartido,
        String uriPartido,
        String siglaUf,
        Integer idLegislatura,
        String urlFoto,
        String email
) {
}
