package org.politicaaberta.web;

import java.time.Instant;

import org.politicaaberta.importacao.deputados.ExecucaoImportacao;

public record ExecucaoImportacaoResposta(
        long id,
        String tipo,
        String status,
        Instant iniciadaEm,
        Instant terminadaEm,
        int quantidade,
        String mensagemErro
) {
    public static ExecucaoImportacaoResposta de(ExecucaoImportacao execucao) {
        return new ExecucaoImportacaoResposta(
                execucao.id(),
                execucao.tipo(),
                execucao.status().codigo(),
                execucao.iniciadaEm(),
                execucao.terminadaEm(),
                execucao.quantidade(),
                execucao.mensagemErro());
    }
}
