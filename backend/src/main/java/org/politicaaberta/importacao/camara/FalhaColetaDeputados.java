package org.politicaaberta.importacao.camara;

import java.net.URI;

/**
 * A coleta nao produziu uma lista confiavel. Nao deve ser tratada
 * como pagina vazia.
 */
public final class FalhaColetaDeputados extends RuntimeException {

    private final URI uri;
    private final Integer statusHttp;

    private FalhaColetaDeputados(URI uri, Integer statusHttp, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.uri = uri;
        this.statusHttp = statusHttp;
    }

    public static FalhaColetaDeputados http(URI uri, int status) {
        return new FalhaColetaDeputados(
                uri,
                status,
                "HTTP " + status + " em " + uri,
                null);
    }

    public static FalhaColetaDeputados contrato(URI uri, String detalhe) {
        return contrato(uri, null, detalhe);
    }

    public static FalhaColetaDeputados contrato(URI uri, Integer status, String detalhe) {
        return new FalhaColetaDeputados(
                uri,
                status,
                detalhe + " em " + uri,
                null);
    }

    public static FalhaColetaDeputados transporte(URI uri, Throwable causa) {
        return new FalhaColetaDeputados(
                uri,
                null,
                "falha de transporte em " + uri,
                causa);
    }

    public URI uri() {
        return uri;
    }

    public Integer statusHttp() {
        return statusHttp;
    }
}
