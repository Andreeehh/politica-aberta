package org.politicaaberta.web;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.politicaaberta.importacao.camara.ClienteDeputadosCamara;
import org.politicaaberta.parlamentares.domain.Deputado;
import org.politicaaberta.parlamentares.domain.DeputadoRepositorio;
import org.politicaaberta.parlamentares.domain.FiltroDeputados;
import org.politicaaberta.parlamentares.domain.RecorteDeputados;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeputadoController.class)
@Import(TratamentoDeErros.class)
class DeputadoControllerTest {

    private static final Instant ATUALIZADO = Instant.parse("2026-10-26T18:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectProvider<ClienteDeputadosCamara> clientesCamara;

    @MockitoBean
    private DeputadoRepositorio deputados;

    @Test
    void listaPaginaPadraoSemChamarACamara() throws Exception {
        when(deputados.consultar(FiltroDeputados.nenhum(), 0, 20))
                .thenReturn(new RecorteDeputados(List.of(exemplo(10, "SP", "PT")), 41));

        mockMvc.perform(get("/api/deputados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.total").value(41))
                .andExpect(jsonPath("$.dados[0].id").value(10))
                .andExpect(jsonPath("$.dados[0].siglaUf").value("SP"));

        assertNull(clientesCamara.getIfAvailable());
    }

    @Test
    void filtraPorUf() throws Exception {
        when(deputados.consultar(new FiltroDeputados("SP", null), 0, 20))
                .thenReturn(new RecorteDeputados(List.of(), 0));

        mockMvc.perform(get("/api/deputados").param("siglaUf", "SP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void filtraPorPartido() throws Exception {
        when(deputados.consultar(new FiltroDeputados(null, "PT"), 20, 20))
                .thenReturn(new RecorteDeputados(List.of(exemplo(8, "RJ", "PT")), 21));

        mockMvc.perform(get("/api/deputados").param("pagina", "2").param("siglaPartido", "PT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(2))
                .andExpect(jsonPath("$.dados[0].siglaPartido").value("PT"));
    }

    @Test
    void combinaFiltros() throws Exception {
        when(deputados.consultar(new FiltroDeputados("MG", "MDB"), 0, 5))
                .thenReturn(new RecorteDeputados(List.of(exemplo(3, "MG", "MDB")), 1));

        mockMvc.perform(get("/api/deputados")
                        .param("tamanho", "5")
                        .param("siglaUf", "MG")
                        .param("siglaPartido", "MDB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados[0].id").value(3));
    }

    @Test
    void consultaPorId() throws Exception {
        when(deputados.buscarPorIdCamara(10)).thenReturn(Optional.of(exemplo(10, "SP", "PT")));

        mockMvc.perform(get("/api/deputados/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nome").value("Pessoa 10"))
                .andExpect(jsonPath("$.atualizadoEm").value("2026-10-26T18:00:00Z"));
    }

    @Test
    void idInexistenteRetorna404() throws Exception {
        when(deputados.buscarPorIdCamara(404)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/deputados/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    void rejeitaParametrosInvalidos() throws Exception {
        mockMvc.perform(get("/api/deputados").param("pagina", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/deputados").param("tamanho", "101"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/deputados").param("pagina", "abc"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/deputados").param("siglaUf", " "))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/deputados/0"))
                .andExpect(status().isBadRequest());
    }

    private static Deputado exemplo(long id, String uf, String partido) {
        return new Deputado(id, "https://exemplo.test/" + id, "Pessoa " + id, partido, uf, 57, null, null, ATUALIZADO);
    }
}
