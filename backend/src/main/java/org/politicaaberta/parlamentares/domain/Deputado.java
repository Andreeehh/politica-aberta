package org.politicaaberta.parlamentares.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Deputado federal como a lista oficial o entrega hoje.
 * {@code nome} e o campo unico da lista: nao ha nome civil
 * nem nome parlamentar separados nesse cliente.
 */
public record Deputado(
        long idCamara,
        String uri,
        String nome,
        String siglaPartido,
        String siglaUf,
        Integer idLegislatura,
        String urlFoto,
        String email,
        Instant atualizadoEm
) {
    public Deputado {
        Objects.requireNonNull(atualizadoEm, "atualizadoEm");
    }
}
