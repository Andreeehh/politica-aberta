package org.politicaaberta.importacao.deputados;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
class ExecucaoImportacaoRepositorioIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.11");

    @Autowired
    private ExecucaoImportacaoRepositorio execucoes;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void registraInicioConclusaoEImpedeOutraAtiva() {
        Instant inicio = Instant.parse("2026-10-26T18:00:00Z");
        ExecucaoImportacao ativa = execucoes.iniciar("deputados", inicio);
        assertEquals(StatusImportacao.EM_EXECUCAO, ativa.status());
        assertThrows(ImportacaoEmAndamento.class, () -> execucoes.iniciar("deputados", inicio));

        ExecucaoImportacao concluida = execucoes.concluir(ativa.id(), 3, inicio.plusSeconds(2));
        assertEquals(StatusImportacao.CONCLUIDA, concluida.status());
        assertEquals(3, concluida.quantidade());
        assertEquals(inicio.plusSeconds(2), concluida.terminadaEm());

        ExecucaoImportacao seguinte = execucoes.iniciar("deputados", inicio.plusSeconds(3));
        ExecucaoImportacao falha = execucoes.falhar(seguinte.id(), 1, inicio.plusSeconds(4), "HTTP 500 em https://exemplo.test");
        assertEquals(StatusImportacao.FALHOU, falha.status());
        assertEquals("HTTP 500 em https://exemplo.test", falha.mensagemErro());
        assertEquals(2L, jdbc.queryForObject("SELECT count(*) FROM execucao_importacao", Long.class));
    }
}
