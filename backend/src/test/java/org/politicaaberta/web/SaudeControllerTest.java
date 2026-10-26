package org.politicaaberta.web;

import org.junit.jupiter.api.Test;
import org.politicaaberta.importacao.camara.ClienteDeputadosCamara;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SaudeController.class)
class SaudeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectProvider<DataSource> fontesDeDados;

    @Autowired
    private ObjectProvider<ClienteDeputadosCamara> clientesCamara;

    @Test
    void healthRetornaStatusUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void healthNaoDependeDeBancoNemDoClienteDaCamara() {
        assertNull(fontesDeDados.getIfAvailable());
        assertNull(clientesCamara.getIfAvailable());
    }
}
