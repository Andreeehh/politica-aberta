package org.politicaaberta.importacao.deputados;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import org.politicaaberta.importacao.camara.ClienteDeputadosCamara;
import org.politicaaberta.parlamentares.domain.DeputadoRepositorio;

/**
 * Percorre a lista oficial e grava cada deputado antes da pagina seguinte.
 * Nao apaga quem ficou de fora. Nao espera Retry-After: o cliente atual
 * nao faz isso, e a fonte repete esse cabecalho tambem em HTTP 200.
 */
public final class ImportacaoDeputados {

    public static final String TIPO = "deputados";
    private static final int LIMITE_MENSAGEM = 500;

    private final ClienteDeputadosCamara cliente;
    private final DeputadoRepositorio deputados;
    private final ExecucaoImportacaoRepositorio execucoes;
    private final Clock relogio;

    public ImportacaoDeputados(
            ClienteDeputadosCamara cliente,
            DeputadoRepositorio deputados,
            ExecucaoImportacaoRepositorio execucoes,
            Clock relogio
    ) {
        this.cliente = Objects.requireNonNull(cliente, "cliente");
        this.deputados = Objects.requireNonNull(deputados, "deputados");
        this.execucoes = Objects.requireNonNull(execucoes, "execucoes");
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    public ExecucaoImportacao executar() {
        Instant inicio = relogio.instant();
        ExecucaoImportacao execucao = execucoes.iniciar(TIPO, inicio);
        int[] processados = {0};
        try {
            cliente.percorrer(item -> {
                deputados.salvar(ConversorDeputado.paraDeputado(item, inicio));
                processados[0]++;
            });
            return execucoes.concluir(execucao.id(), processados[0], relogio.instant());
        } catch (RuntimeException erro) {
            ExecucaoImportacao falha = registrarFalha(execucao.id(), processados[0], erro);
            throw new FalhaImportacaoDeputados(falha, erro);
        }
    }

    private ExecucaoImportacao registrarFalha(long id, int processados, RuntimeException erro) {
        try {
            return execucoes.falhar(id, processados, relogio.instant(), resumir(erro));
        } catch (RuntimeException registro) {
            erro.addSuppressed(registro);
            throw erro;
        }
    }

    private static String resumir(RuntimeException erro) {
        String texto = erro.getMessage();
        if (texto == null || texto.isBlank()) {
            texto = erro.getClass().getSimpleName();
        }
        texto = texto.replaceAll("\\s+", " ").trim();
        if (texto.length() <= LIMITE_MENSAGEM) {
            return texto;
        }
        return texto.substring(0, LIMITE_MENSAGEM);
    }
}
