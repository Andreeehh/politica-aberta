package org.politicaaberta.importacao.camara;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.politicaaberta.importacao.http.RespostaHttp;
import org.politicaaberta.importacao.http.TransporteHttp;
import org.politicaaberta.importacao.http.TransporteHttpJdk;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coleta real. Nao entra em {@code mvn test}.
 * Comando: {@code mvn -f backend/pom.xml test -Pintegracao}
 */
class ClienteDeputadosCamaraIT {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void coletaCompletaNaApiOficial() throws Exception {
        Registrador transporte = new Registrador(new TransporteHttpJdk());
        ResultadoListaDeputados resultado = new ClienteDeputadosCamara(transporte).listar();

        List<PaginaObservada> paginas = observar(transporte.chamadas());
        Relatorio relatorio = relatorio(resultado, paginas);
        System.out.println(relatorio.texto());

        assertFalse(paginas.isEmpty(), relatorio.texto());
        assertEquals(ClienteDeputadosCamara.URL_LISTA, paginas.getFirst().uri(), relatorio.texto());
        for (int i = 1; i < paginas.size(); i++) {
            assertEquals(paginas.get(i - 1).proxima(), paginas.get(i).uri(), relatorio.texto());
        }
        PaginaObservada ultima = paginas.getLast();
        assertTrue(ultima.vazia() || ultima.proxima() == null, relatorio.texto());
        assertEquals(relatorio.idsUnicos(), resultado.deputados().size(), relatorio.texto());
        assertNotNull(resultado.totalConhecido(), relatorio.texto());
        assertTrue(resultado.totalConhecido() > 0, relatorio.texto());
        assertEquals(relatorio.totalDaPrimeiraPaginaComDado(), resultado.totalConhecido(), relatorio.texto());
        if (ultima.vazia()) {
            assertTrue(ultima.totalInformado() == null || ultima.totalInformado() == 0L, relatorio.texto());
            assertEquals(relatorio.totalDaPrimeiraPaginaComDado(), resultado.totalConhecido(), relatorio.texto());
        }
        assertEquals(relatorio.totalDaPrimeiraPaginaComDado(), (long) relatorio.idsUnicos(), relatorio.texto());
    }

    private static List<PaginaObservada> observar(List<Chamada> chamadas) throws Exception {
        List<PaginaObservada> paginas = new ArrayList<>();
        for (Chamada chamada : chamadas) {
            if (chamada.status() != 200) {
                throw new AssertionError("HTTP " + chamada.status() + " em " + chamada.uri());
            }
            JsonNode raiz = JSON.readTree(chamada.corpo());
            if (!raiz.isObject() || !raiz.path("dados").isArray()) {
                throw new AssertionError("JSON incompativel em " + chamada.uri());
            }
            List<Long> ids = new ArrayList<>();
            for (JsonNode item : raiz.path("dados")) {
                if (!item.path("id").isIntegralNumber()) {
                    throw new AssertionError("id ausente em " + chamada.uri());
                }
                ids.add(item.path("id").longValue());
            }
            URI proxima = null;
            for (JsonNode link : raiz.path("links")) {
                if ("next".equalsIgnoreCase(link.path("rel").asText())) {
                    proxima = chamada.uri().resolve(URI.create(link.path("href").asText().trim()));
                }
            }
            Long total = null;
            if (chamada.total() != null) {
                total = Long.parseLong(chamada.total().trim());
            }
            paginas.add(new PaginaObservada(chamada.uri(), ids, proxima, total));
        }
        return paginas;
    }

    private static Relatorio relatorio(ResultadoListaDeputados resultado, List<PaginaObservada> paginas) {
        Map<Long, Integer> frequencia = new LinkedHashMap<>();
        Long totalPrimeira = null;
        for (PaginaObservada pagina : paginas) {
            if (totalPrimeira == null && !pagina.vazia() && pagina.totalInformado() != null && pagina.totalInformado() > 0) {
                totalPrimeira = pagina.totalInformado();
            }
            for (Long id : pagina.ids()) {
                frequencia.merge(id, 1, Integer::sum);
            }
        }
        int idsUnicos = frequencia.size();
        int identificadoresDuplicados = (int) frequencia.values().stream().filter(vezes -> vezes > 1).count();
        int ocorrenciasExtras = frequencia.values().stream().mapToInt(vezes -> vezes - 1).sum();
        boolean ultimaVazia = paginas.getLast().vazia();
        String texto = """
                Coleta da API oficial
                deputados unicos: %d
                total conhecido: %s
                requisicoes HTTP: %d
                identificadores duplicados: %d
                ocorrencias extras: %d
                terminou normalmente: sim
                pagina vazia final: %s
                total da pagina vazia: %s
                """.formatted(
                idsUnicos,
                resultado.totalConhecido(),
                paginas.size(),
                identificadoresDuplicados,
                ocorrenciasExtras,
                ultimaVazia ? "sim" : "nao; a ultima pagina com deputados nao trouxe rel=next",
                ultimaVazia ? String.valueOf(paginas.getLast().totalInformado()) : "nao consultada");
        return new Relatorio(texto, idsUnicos, totalPrimeira);
    }

    private record Relatorio(String texto, int idsUnicos, Long totalDaPrimeiraPaginaComDado) {
    }

    private record PaginaObservada(URI uri, List<Long> ids, URI proxima, Long totalInformado) {
        boolean vazia() {
            return ids.isEmpty();
        }
    }

    private record Chamada(URI uri, int status, String total, String corpo) {
    }

    private static final class Registrador implements TransporteHttp {
        private final TransporteHttp delegado;
        private final List<Chamada> chamadas = new ArrayList<>();

        private Registrador(TransporteHttp delegado) {
            this.delegado = delegado;
        }

        private List<Chamada> chamadas() {
            return List.copyOf(chamadas);
        }

        @Override
        public RespostaHttp get(URI uri) throws IOException {
            RespostaHttp resposta = delegado.get(uri);
            chamadas.add(new Chamada(
                    uri,
                    resposta.status(),
                    resposta.primeiroCabecalho("X-Total-Count"),
                    resposta.corpo()));
            return resposta;
        }
    }
}
