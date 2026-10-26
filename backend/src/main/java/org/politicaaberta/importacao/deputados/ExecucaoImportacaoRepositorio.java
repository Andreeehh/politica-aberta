package org.politicaaberta.importacao.deputados;

import java.time.Instant;

public interface ExecucaoImportacaoRepositorio {

    ExecucaoImportacao iniciar(String tipo, Instant inicio);

    ExecucaoImportacao concluir(long id, int quantidade, Instant termino);

    ExecucaoImportacao falhar(long id, int quantidade, Instant termino, String mensagem);
}
