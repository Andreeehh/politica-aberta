package org.politicaaberta.parlamentares.infrastructure;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.politicaaberta.parlamentares.domain.Deputado;
import org.politicaaberta.parlamentares.domain.DeputadoRepositorio;
import org.politicaaberta.parlamentares.domain.FiltroDeputados;
import org.politicaaberta.parlamentares.domain.RecorteDeputados;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class DeputadoRepositorioIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.11");

    @Autowired
    private DeputadoRepositorio repositorio;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limpar() {
        jdbc.update("DELETE FROM deputado");
    }

    @Test
    void gravaEConsultaPeloIdOficial() {
        Instant atualizado = Instant.parse("2026-10-26T15:00:00Z");
        repositorio.salvar(new Deputado(
                101,
                "https://dadosabertos.camara.leg.br/api/v2/deputados/101",
                "Pessoa Alfa",
                "AAA",
                "SP",
                57,
                "https://exemplo.test/101.jpg",
                "alfa@exemplo.test",
                atualizado));

        Deputado lido = repositorio.buscarPorIdCamara(101).orElseThrow();
        assertEquals(101, lido.idCamara());
        assertEquals("Pessoa Alfa", lido.nome());
        assertEquals("AAA", lido.siglaPartido());
        assertEquals("SP", lido.siglaUf());
        assertEquals(57, lido.idLegislatura());
        assertEquals("alfa@exemplo.test", lido.email());
        assertEquals(atualizado, lido.atualizadoEm());
        assertTrue(repositorio.buscarPorIdCamara(999).isEmpty());
    }

    @Test
    void listaEmOrdemCrescenteDeId() {
        Instant atualizado = Instant.parse("2026-10-26T15:00:00Z");
        repositorio.salvar(minimo(30, atualizado));
        repositorio.salvar(minimo(10, atualizado));
        repositorio.salvar(minimo(20, atualizado));

        List<Long> ids = repositorio.listar().stream().map(Deputado::idCamara).toList();
        assertEquals(List.of(10L, 20L, 30L), ids);
    }

    @Test
    void repetirOMesmoIdAtualizaSemDuplicar() {
        repositorio.salvar(new Deputado(
                101, "https://exemplo.test/101", "Pessoa Alfa", "AAA", "SP", 57, null, null,
                Instant.parse("2026-10-26T15:00:00Z")));
        repositorio.salvar(new Deputado(
                101, "https://exemplo.test/101b", "Pessoa Alfa Atualizada", "BBB", "RJ", 58, null, "novo@exemplo.test",
                Instant.parse("2026-10-26T16:00:00Z")));

        assertEquals(1L, jdbc.queryForObject(
                "SELECT count(*) FROM deputado WHERE id_camara = 101", Long.class));
        Deputado lido = repositorio.buscarPorIdCamara(101).orElseThrow();
        assertEquals("Pessoa Alfa Atualizada", lido.nome());
        assertEquals("BBB", lido.siglaPartido());
        assertEquals("RJ", lido.siglaUf());
        assertEquals(58, lido.idLegislatura());
        assertEquals("novo@exemplo.test", lido.email());
        assertEquals(Instant.parse("2026-10-26T16:00:00Z"), lido.atualizadoEm());
    }

    @Test
    void rejeitaSegundaLinhaComOMesmoIdEAtualizacaoNula() {
        repositorio.salvar(minimo(101, Instant.parse("2026-10-26T15:00:00Z")));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO deputado (id_camara, atualizado_em) VALUES (?, ?)",
                101,
                OffsetDateTime.ofInstant(Instant.parse("2026-10-26T17:00:00Z"), ZoneOffset.UTC)));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO deputado (id_camara, atualizado_em) VALUES (404, NULL)"));
    }

    @Test
    void consultaPaginaEFiltraPorUfEPartido() {
        Instant atualizado = Instant.parse("2026-10-26T15:00:00Z");
        repositorio.salvar(new Deputado(1, null, "Ana", "PT", "SP", 57, null, null, atualizado));
        repositorio.salvar(new Deputado(2, null, "Bia", "PT", "RJ", 57, null, null, atualizado));
        repositorio.salvar(new Deputado(3, null, "Caio", "MDB", "SP", 57, null, null, atualizado));

        RecorteDeputados uf = repositorio.consultar(new FiltroDeputados("sp", null), 0, 10);
        assertEquals(List.of(1L, 3L), uf.deputados().stream().map(Deputado::idCamara).toList());
        assertEquals(2, uf.total());

        RecorteDeputados partido = repositorio.consultar(new FiltroDeputados(null, "pt"), 0, 10);
        assertEquals(List.of(1L, 2L), partido.deputados().stream().map(Deputado::idCamara).toList());

        RecorteDeputados ambos = repositorio.consultar(new FiltroDeputados("SP", "PT"), 0, 10);
        assertEquals(List.of(1L), ambos.deputados().stream().map(Deputado::idCamara).toList());
        assertEquals(1, ambos.total());

        RecorteDeputados pagina = repositorio.consultar(FiltroDeputados.nenhum(), 1, 1);
        assertEquals(List.of(2L), pagina.deputados().stream().map(Deputado::idCamara).toList());
        assertEquals(3, pagina.total());
    }

    private static Deputado minimo(long id, Instant atualizado) {
        return new Deputado(id, null, "Pessoa " + id, null, null, null, null, null, atualizado);
    }
}
