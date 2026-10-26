package org.politicaaberta.parlamentares.domain;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeputadoTest {

    @Test
    void exigeInstanteDeAtualizacao() {
        assertThrows(NullPointerException.class, () -> new Deputado(
                101, null, "Pessoa Alfa", null, null, null, null, null, null));
    }

    @Test
    void aceitaCamposOpcionaisNulos() {
        Instant agora = Instant.parse("2026-10-26T18:00:00Z");
        Deputado deputado = new Deputado(101, null, null, null, null, null, null, null, agora);
        assertEquals(101, deputado.idCamara());
        assertEquals(agora, deputado.atualizadoEm());
    }
}
