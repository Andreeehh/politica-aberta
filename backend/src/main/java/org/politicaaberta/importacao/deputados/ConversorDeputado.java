package org.politicaaberta.importacao.deputados;

import java.time.Instant;
import java.util.Objects;

import org.politicaaberta.importacao.camara.DeputadoEmLista;
import org.politicaaberta.parlamentares.domain.Deputado;

public final class ConversorDeputado {

    private ConversorDeputado() {
    }

    public static Deputado paraDeputado(DeputadoEmLista origem, Instant atualizadoEm) {
        Objects.requireNonNull(origem, "origem");
        return new Deputado(
                origem.id(),
                origem.uri(),
                origem.nome(),
                origem.siglaPartido(),
                origem.siglaUf(),
                origem.idLegislatura(),
                origem.urlFoto(),
                origem.email(),
                atualizadoEm);
    }
}
