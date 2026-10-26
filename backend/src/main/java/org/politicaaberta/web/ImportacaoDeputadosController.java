package org.politicaaberta.web;

import org.politicaaberta.importacao.deputados.ImportacaoDeputados;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dispara a coleta sob demanda. Nao ha autenticacao:
 * nao exponha este caminho em producao antes do controle de acesso.
 * A chamada bloqueia ate a coleta terminar. A subida da aplicacao nao a executa.
 */
@RestController
@RequestMapping("/api/admin/importacoes/deputados")
@ConditionalOnProperty(prefix = "politica-aberta", name = "banco", havingValue = "true", matchIfMissing = true)
public class ImportacaoDeputadosController {

    private final ImportacaoDeputados importacao;

    public ImportacaoDeputadosController(ImportacaoDeputados importacao) {
        this.importacao = importacao;
    }

    @PostMapping
    public ExecucaoImportacaoResposta disparar() {
        return ExecucaoImportacaoResposta.de(importacao.executar());
    }
}
