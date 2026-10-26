package org.politicaaberta.importacao.camara;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.politicaaberta.importacao.http.RespostaHttp;
import org.politicaaberta.importacao.http.TransporteHttp;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Le {@code GET /deputados} pagina a pagina.
 * A primeira URL pede {@code itens=100}. As seguintes sao o {@code href}
 * de {@code rel=next}, sem remontar a query.
 */
public final class ClienteDeputadosCamara {

    public static final URI URL_LISTA = URI.create(
            "https://dadosabertos.camara.leg.br/api/v2/deputados?itens=100&ordem=asc&ordenarPor=id");

    private static final ObjectMapper JSON = new ObjectMapper();

    private final TransporteHttp transporte;

    public ClienteDeputadosCamara(TransporteHttp transporte) {
        this.transporte = Objects.requireNonNull(transporte, "transporte");
    }

    public ResultadoListaDeputados listar() {
        List<DeputadoEmLista> acumulado = new ArrayList<>();
        Long total = percorrer(acumulado::add);
        return new ResultadoListaDeputados(acumulado, total);
    }

    /**
     * Entrega cada deputado unico assim que a pagina dele e lida.
     * A pagina seguinte so e pedida depois. O total segue a mesma regra de {@link #listar()}.
     */
    public Long percorrer(Consumer<DeputadoEmLista> visitante) {
        Objects.requireNonNull(visitante, "visitante");
        Set<Long> ids = new HashSet<>();
        Set<URI> visitadas = new HashSet<>();
        Long total = null;
        URI atual = URL_LISTA;

        while (atual != null) {
            if (!visitadas.add(atual)) {
                throw FalhaColetaDeputados.contrato(atual, "link next repete uma URL ja consultada");
            }
            RespostaHttp resposta = pedir(atual);
            if (resposta.status() != 200) {
                throw FalhaColetaDeputados.http(atual, resposta.status());
            }
            Pagina pagina = lerPagina(atual, resposta.corpo());
            if (pagina.deputados().isEmpty()) {
                break;
            }
            if (total == null) {
                Long informado = totalUtilizavel(resposta.primeiroCabecalho("X-Total-Count"));
                if (informado != null && informado > 0) {
                    total = informado;
                }
            }
            for (DeputadoEmLista deputado : pagina.deputados()) {
                if (ids.add(deputado.id())) {
                    visitante.accept(deputado);
                }
            }
            atual = pagina.proxima();
        }
        return total;
    }

    private RespostaHttp pedir(URI uri) {
        try {
            return transporte.get(uri);
        } catch (IOException erro) {
            throw FalhaColetaDeputados.transporte(uri, erro);
        }
    }

    private static Long totalUtilizavel(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            long valor = Long.parseLong(texto.trim());
            if (valor < 0) {
                return null;
            }
            return valor;
        } catch (NumberFormatException erro) {
            return null;
        }
    }

    private static Pagina lerPagina(URI uri, String corpo) {
        JsonNode raiz = lerJson(uri, corpo);
        if (!raiz.isObject()) {
            throw FalhaColetaDeputados.contrato(uri, 200, "corpo nao e um objeto JSON");
        }
        JsonNode dados = raiz.get("dados");
        if (dados == null || !dados.isArray()) {
            throw FalhaColetaDeputados.contrato(uri, 200, "campo dados ausente ou invalido");
        }
        List<DeputadoEmLista> deputados = new ArrayList<>();
        for (JsonNode item : dados) {
            deputados.add(lerDeputado(uri, item));
        }
        return new Pagina(deputados, lerProxima(uri, raiz.get("links")));
    }

    private static JsonNode lerJson(URI uri, String corpo) {
        try {
            JsonNode raiz = JSON.readTree(corpo);
            if (raiz == null || raiz.isNull()) {
                throw FalhaColetaDeputados.contrato(uri, 200, "JSON vazio");
            }
            return raiz;
        } catch (JsonProcessingException erro) {
            throw FalhaColetaDeputados.contrato(uri, 200, "JSON invalido");
        }
    }

    private static DeputadoEmLista lerDeputado(URI uri, JsonNode item) {
        if (item == null || !item.isObject()) {
            throw FalhaColetaDeputados.contrato(uri, 200, "item de dados nao e um objeto");
        }
        return new DeputadoEmLista(
                numeroLongo(uri, item.get("id"), "id"),
                textoOpcional(uri, item.get("uri"), "uri"),
                textoOpcional(uri, item.get("nome"), "nome"),
                textoOpcional(uri, item.get("siglaPartido"), "siglaPartido"),
                textoOpcional(uri, item.get("uriPartido"), "uriPartido"),
                textoOpcional(uri, item.get("siglaUf"), "siglaUf"),
                legislatura(uri, item.get("idLegislatura")),
                textoOpcional(uri, item.get("urlFoto"), "urlFoto"),
                textoOpcional(uri, item.get("email"), "email"));
    }

    private static long numeroLongo(URI uri, JsonNode no, String campo) {
        if (no == null || no.isNull() || !no.isIntegralNumber() || !no.canConvertToLong()) {
            throw FalhaColetaDeputados.contrato(uri, 200, "campo " + campo + " ausente ou invalido");
        }
        return no.longValue();
    }

    private static Integer legislatura(URI uri, JsonNode no) {
        if (no == null || no.isNull()) {
            return null;
        }
        if (!no.isIntegralNumber() || !no.canConvertToInt()) {
            throw FalhaColetaDeputados.contrato(uri, 200, "campo idLegislatura invalido");
        }
        return no.intValue();
    }

    private static String textoOpcional(URI uri, JsonNode no, String campo) {
        if (no == null || no.isNull()) {
            return null;
        }
        if (!no.isTextual()) {
            throw FalhaColetaDeputados.contrato(uri, 200, "campo " + campo + " invalido");
        }
        return no.textValue();
    }

    private static URI lerProxima(URI atual, JsonNode links) {
        if (links == null || links.isNull()) {
            return null;
        }
        if (!links.isArray()) {
            throw FalhaColetaDeputados.contrato(atual, 200, "campo links invalido");
        }
        for (JsonNode link : links) {
            if (link == null || !link.isObject()) {
                throw FalhaColetaDeputados.contrato(atual, 200, "link invalido");
            }
            JsonNode rel = link.get("rel");
            if (rel == null || rel.isNull()) {
                continue;
            }
            if (!rel.isTextual()) {
                throw FalhaColetaDeputados.contrato(atual, 200, "rel de link invalido");
            }
            if (!"next".equalsIgnoreCase(rel.textValue())) {
                continue;
            }
            JsonNode href = link.get("href");
            if (href == null || !href.isTextual() || href.textValue().isBlank()) {
                throw FalhaColetaDeputados.contrato(atual, 200, "href de next ausente");
            }
            try {
                return atual.resolve(URI.create(href.textValue().trim()));
            } catch (IllegalArgumentException erro) {
                throw FalhaColetaDeputados.contrato(atual, 200, "href de next invalido");
            }
        }
        return null;
    }

    private record Pagina(List<DeputadoEmLista> deputados, URI proxima) {
    }
}
