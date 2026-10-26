package org.politicaaberta.importacao.deputados;

import java.time.Clock;

import org.politicaaberta.importacao.camara.ClienteDeputadosCamara;
import org.politicaaberta.parlamentares.domain.DeputadoRepositorio;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration
public class ConfiguracaoImportacao {

    @Bean
    Clock relogio() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnProperty(prefix = "politica-aberta", name = "banco", havingValue = "true", matchIfMissing = true)
    ExecucaoImportacaoRepositorio execucaoImportacaoRepositorio(JdbcClient jdbc) {
        return new ExecucaoImportacaoRepositorioJdbc(jdbc);
    }

    @Bean
    @ConditionalOnProperty(prefix = "politica-aberta", name = "banco", havingValue = "true", matchIfMissing = true)
    ImportacaoDeputados importacaoDeputados(
            ClienteDeputadosCamara cliente,
            DeputadoRepositorio deputados,
            ExecucaoImportacaoRepositorio execucoes,
            Clock relogio
    ) {
        return new ImportacaoDeputados(cliente, deputados, execucoes, relogio);
    }
}
