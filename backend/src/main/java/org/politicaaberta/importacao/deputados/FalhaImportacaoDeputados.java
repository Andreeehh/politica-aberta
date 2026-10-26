package org.politicaaberta.importacao.deputados;

public final class FalhaImportacaoDeputados extends RuntimeException {

    private final ExecucaoImportacao execucao;

    public FalhaImportacaoDeputados(ExecucaoImportacao execucao, Throwable causa) {
        super(execucao.mensagemErro(), causa);
        this.execucao = execucao;
    }

    public ExecucaoImportacao execucao() {
        return execucao;
    }
}
