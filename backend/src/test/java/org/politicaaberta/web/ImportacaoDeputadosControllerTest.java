package org.politicaaberta.web;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.politicaaberta.importacao.camara.FalhaColetaDeputados;
import org.politicaaberta.importacao.deputados.ExecucaoImportacao;
import org.politicaaberta.importacao.deputados.FalhaImportacaoDeputados;
import org.politicaaberta.importacao.deputados.ImportacaoDeputados;
import org.politicaaberta.importacao.deputados.ImportacaoEmAndamento;
import org.politicaaberta.importacao.deputados.StatusImportacao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImportacaoDeputadosController.class)
@Import(TratamentoDeErros.class)
class ImportacaoDeputadosControllerTest {

    private static final Instant INICIO = Instant.parse("2026-10-26T18:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImportacaoDeputados importacao;

    @Test
    void disparoConcluidoDevolveORegistro() throws Exception {
        when(importacao.executar()).thenReturn(new ExecucaoImportacao(
                4, "deputados", INICIO, INICIO, StatusImportacao.CONCLUIDA, 3, null));

        mockMvc.perform(post("/api/admin/importacoes/deputados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.status").value("concluida"))
                .andExpect(jsonPath("$.quantidade").value(3))
                .andExpect(jsonPath("$.mensagemErro").value(nullValue()));
    }

    @Test
    void conflitoQuandoJaExisteExecucao() throws Exception {
        when(importacao.executar()).thenThrow(new ImportacaoEmAndamento("deputados"));

        mockMvc.perform(post("/api/admin/importacoes/deputados"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").exists())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void falhaDaFonteDevolve502SemRastreamento() throws Exception {
        ExecucaoImportacao falha = new ExecucaoImportacao(
                5, "deputados", INICIO, INICIO, StatusImportacao.FALHOU, 2, "HTTP 500 em https://exemplo.test");
        when(importacao.executar()).thenThrow(new FalhaImportacaoDeputados(
                falha, FalhaColetaDeputados.http(java.net.URI.create("https://exemplo.test"), 500)));

        mockMvc.perform(post("/api/admin/importacoes/deputados"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value("falhou"))
                .andExpect(jsonPath("$.quantidade").value(2))
                .andExpect(jsonPath("$.mensagemErro").value("HTTP 500 em https://exemplo.test"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }
}
