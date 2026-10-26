package org.politicaaberta.parlamentares.domain;

import java.util.List;

public record RecorteDeputados(List<Deputado> deputados, long total) {

    public RecorteDeputados {
        deputados = List.copyOf(deputados);
    }
}
