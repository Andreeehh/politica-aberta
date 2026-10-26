package org.politicaaberta.importacao.camara;

import org.junit.jupiter.api.Test;
import org.politicaaberta.importacao.http.RespostaHttp;
import org.politicaaberta.importacao.http.TransporteHttp;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClienteDeputadosCamaraTest {

    private static final URI PAGINA_2 = URI.create(
            "https://dadosabertos.camara.leg.br/api/v2/deputados?ordem=asc&ordenarPor=id&pagina=2&itens=100");
    private static final URI PAGINA_3 = URI.create(
            "https://dadosabertos.camara.leg.br/api/v2/deputados?ordem=asc&ordenarPor=id&pagina=3&itens=100");
    private static final URI PAGINA_4 = URI.create(
            "https://dadosabertos.camara.leg.br/api/v2/deputados?pagina=4&itens=100");

    @Test
    void umaPaginaDevolveDeputadosETotal() {
        TransporteFixo transporte = new TransporteFixo()
                .resposta(ClienteDeputadosCamara.URL_LISTA, 200, "3", fixture("pagina-unica.json"));

        ResultadoListaDeputados resultado = new ClienteDeputadosCamara(transporte).listar();

        assertEquals(List.of(201L), ids(resultado));
        assertEquals("Pessoa Unica", resultado.deputados().getFirst().nome());
        assertEquals(57, resultado.deputados().getFirst().idLegislatura());
        assertEquals(3L, resultado.totalConhecido());
        assertEquals(List.of(ClienteDeputadosCamara.URL_LISTA), transporte.chamadas());
    }

    @Test
    void paginasSeguemRelNextAtePaginaVaziaEPreservamTotal() {
        TransporteFixo transporte = new TransporteFixo()
                .resposta(ClienteDeputadosCamara.URL_LISTA, 200, "3", fixture("pagina-1.json"))
                .resposta(PAGINA_2, 200, "999", fixture("pagina-2.json"))
                .resposta(PAGINA_3, 200, "0", fixture("pagina-vazia.json"));

        ResultadoListaDeputados resultado = new ClienteDeputadosCamara(transporte).listar();

        assertEquals(List.of(101L, 102L, 103L), ids(resultado));
        assertEquals("Pessoa Beta", resultado.deputados().get(1).nome());
        assertEquals(3L, resultado.totalConhecido());
        assertEquals(List.of(ClienteDeputadosCamara.URL_LISTA, PAGINA_2, PAGINA_3), transporte.chamadas());
        assertTrue(transporte.chamadas().stream().noneMatch(PAGINA_4::equals));
    }

    @Test
    void ausenciaDeTotalNaoImpedeAColeta() {
        TransporteFixo transporte = new TransporteFixo()
                .resposta(ClienteDeputadosCamara.URL_LISTA, 200, null, fixture("pagina-unica.json"));

        ResultadoListaDeputados resultado = new ClienteDeputadosCamara(transporte).listar();

        assertEquals(List.of(201L), ids(resultado));
        assertNull(resultado.totalConhecido());
    }

    @Test
    void ausenciaDeRelNextEncerraNaPaginaComDados() {
        TransporteFixo transporte = new TransporteFixo()
                .resposta(ClienteDeputadosCamara.URL_LISTA, 200, "1", fixture("sem-links.json"));

        ResultadoListaDeputados resultado = new ClienteDeputadosCamara(transporte).listar();

        assertEquals(List.of(301L), ids(resultado));
        assertEquals(1, transporte.chamadas().size());
    }

    @Test
    void erroHttpNoMeioDaPaginacaoNaoViraListaVazia() {
        TransporteFixo transporte = new TransporteFixo()
                .resposta(ClienteDeputadosCamara.URL_LISTA, 200, "3", fixture("pagina-1.json"))
                .resposta(PAGINA_2, 502, "0", "");

        FalhaColetaDeputados erro = assertThrows(
                FalhaColetaDeputados.class,
                () -> new ClienteDeputadosCamara(transporte).listar());

        assertEquals(502, erro.statusHttp());
        assertEquals(PAGINA_2, erro.uri());
    }

    @Test
    void jsonInvalidoFalhaAColeta() {
        TransporteFixo transporte = new TransporteFixo()
                .resposta(ClienteDeputadosCamara.URL_LISTA, 200, "1", fixture("json-invalido.txt"));

        FalhaColetaDeputados erro = assertThrows(
                FalhaColetaDeputados.class,
                () -> new ClienteDeputadosCamara(transporte).listar());

        assertEquals(200, erro.statusHttp());
        assertTrue(erro.getMessage().contains("JSON invalido"));
    }

    @Test
    void contratoIncompativelFalhaAColeta() {
        TransporteFixo transporte = new TransporteFixo()
                .resposta(ClienteDeputadosCamara.URL_LISTA, 200, "1", fixture("dados-invalidos.json"));

        FalhaColetaDeputados erro = assertThrows(
                FalhaColetaDeputados.class,
                () -> new ClienteDeputadosCamara(transporte).listar());

        assertTrue(erro.getMessage().contains("dados"));
    }

    @Test
    void itemSemIdOficialFalhaAColeta() {
        TransporteFixo transporte = new TransporteFixo()
                .resposta(ClienteDeputadosCamara.URL_LISTA, 200, "1", fixture("item-sem-id.json"));

        FalhaColetaDeputados erro = assertThrows(
                FalhaColetaDeputados.class,
                () -> new ClienteDeputadosCamara(transporte).listar());

        assertTrue(erro.getMessage().contains("id"));
    }

    @Test
    void falhaDeTransporteNaoViraListaVazia() {
        TransporteHttp transporte = uri -> {
            throw new IOException("rede indisponivel");
        };

        FalhaColetaDeputados erro = assertThrows(
                FalhaColetaDeputados.class,
                () -> new ClienteDeputadosCamara(transporte).listar());

        assertNull(erro.statusHttp());
        assertTrue(erro.getCause() instanceof IOException);
    }

    private static List<Long> ids(ResultadoListaDeputados resultado) {
        return resultado.deputados().stream().map(DeputadoEmLista::id).toList();
    }

    private static String fixture(String nome) {
        String caminho = "/fixtures/camara/deputados/" + nome;
        try (InputStream entrada = ClienteDeputadosCamaraTest.class.getResourceAsStream(caminho)) {
            if (entrada == null) {
                throw new IllegalStateException("fixture ausente: " + caminho);
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException erro) {
            throw new IllegalStateException(erro);
        }
    }

    private static final class TransporteFixo implements TransporteHttp {
        private final Map<URI, RespostaHttp> respostas = new LinkedHashMap<>();
        private final List<URI> chamadas = new ArrayList<>();

        TransporteFixo resposta(URI uri, int status, String total, String corpo) {
            Map<String, List<String>> cabecalhos = total == null
                    ? Map.of()
                    : Map.of("X-Total-Count", List.of(total));
            respostas.put(uri, new RespostaHttp(status, cabecalhos, corpo));
            return this;
        }

        List<URI> chamadas() {
            return List.copyOf(chamadas);
        }

        @Override
        public RespostaHttp get(URI uri) {
            chamadas.add(uri);
            RespostaHttp resposta = respostas.get(uri);
            if (resposta == null) {
                throw new AssertionError("URL nao prevista: " + uri);
            }
            return resposta;
        }
    }
}
