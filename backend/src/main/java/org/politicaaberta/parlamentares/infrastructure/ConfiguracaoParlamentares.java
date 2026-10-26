package org.politicaaberta.parlamentares.infrastructure;

import org.politicaaberta.parlamentares.domain.DeputadoRepositorio;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration
public class ConfiguracaoParlamentares {

    @Bean
    @ConditionalOnProperty(prefix = "politica-aberta", name = "banco", havingValue = "true", matchIfMissing = true)
    DeputadoRepositorio deputadoRepositorio(JdbcClient jdbc) {
        return new DeputadoRepositorioJdbc(jdbc);
    }
}
