package org.politicaaberta.web;

import java.time.Instant;

import org.politicaaberta.parlamentares.domain.Deputado;

public record DeputadoResposta(
        long id,
        String uri,
        String nome,
        String siglaPartido,
        String siglaUf,
        Integer idLegislatura,
        String urlFoto,
        String email,
        Instant atualizadoEm
) {
    public static DeputadoResposta de(Deputado deputado) {
        return new DeputadoResposta(
                deputado.idCamara(),
                deputado.uri(),
                deputado.nome(),
                deputado.siglaPartido(),
                deputado.siglaUf(),
                deputado.idLegislatura(),
                deputado.urlFoto(),
                deputado.email(),
                deputado.atualizadoEm());
    }
}
