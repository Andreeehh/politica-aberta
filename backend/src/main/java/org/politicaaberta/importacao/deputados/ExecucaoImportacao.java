package org.politicaaberta.importacao.deputados;

import java.time.Instant;

public record ExecucaoImportacao(
        long id,
        String tipo,
        Instant iniciadaEm,
        Instant terminadaEm,
        StatusImportacao status,
        int quantidade,
        String mensagemErro
) {
}
