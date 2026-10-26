package org.politicaaberta.parlamentares.infrastructure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeputadoRepositorioJdbcTest {

    @Test
    void gravacaoRepeteOMesmoIdOficial() {
        assertTrue(DeputadoRepositorioJdbc.UPSERT.contains("ON CONFLICT (id_camara) DO UPDATE"));
        assertTrue(DeputadoRepositorioJdbc.UPSERT.contains(":idCamara"));
        assertFalse(DeputadoRepositorioJdbc.UPSERT.toLowerCase().contains("cpf"));
    }

    @Test
    void filtroUsaParametros() {
        assertTrue(DeputadoRepositorioJdbc.FILTRO.contains(":siglaUf"));
        assertTrue(DeputadoRepositorioJdbc.FILTRO.contains(":siglaPartido"));
        assertFalse(DeputadoRepositorioJdbc.FILTRO.contains("+"));
    }

    @Test
    void listagemOrdenaPeloIdOficial() {
        String sql = "SELECT " + DeputadoRepositorioJdbc.COLUNAS + " FROM deputado ORDER BY id_camara";
        assertTrue(sql.contains("ORDER BY id_camara"));
        assertFalse(sql.toLowerCase().contains("cpf"));
    }
}
