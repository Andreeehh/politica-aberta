package org.politicaaberta.parlamentares.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigracaoDeputadoTest {

    @Test
    void baselineContinuaSemTabela() throws IOException {
        String v1 = texto("db/migration/V1__baseline.sql");
        assertTrue(v1.contains("SELECT 1"));
        assertFalse(v1.toLowerCase().contains("create table"));
    }

    @Test
    void tabelaUsaIdOficialComoChaveENaoGuardaCpf() throws IOException {
        String v2 = texto("db/migration/V2__criar_deputado.sql").toLowerCase();
        String definicao = v2.substring(v2.indexOf("create table"), v2.indexOf("comment on"));
        assertTrue(definicao.contains("create table deputado"));
        assertTrue(definicao.contains("id_camara bigint primary key"));
        assertTrue(definicao.contains("atualizado_em timestamptz not null"));
        assertTrue(definicao.contains("nome text"));
        assertTrue(definicao.contains("email text"));
        assertFalse(definicao.contains("cpf"));
        assertFalse(definicao.contains("json"));
        assertFalse(definicao.contains("nome_civil"));
        assertFalse(definicao.contains("nome_parlamentar"));
    }

    private static String texto(String caminho) throws IOException {
        return new ClassPathResource(caminho).getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void versoesContinuamEmOrdem() {
        assertEquals("V1__baseline.sql", new ClassPathResource("db/migration/V1__baseline.sql").getFilename());
        assertEquals("V2__criar_deputado.sql", new ClassPathResource("db/migration/V2__criar_deputado.sql").getFilename());
    }
}
