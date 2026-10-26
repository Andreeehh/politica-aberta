package org.politicaaberta.importacao.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

/**
 * GET com o cliente HTTP do JDK. Nao segue redirecionamento:
 * a proxima URL tem de ser a que a API colocou em rel=next.
 */
public final class TransporteHttpJdk implements TransporteHttp {

    private final HttpClient cliente;

    public TransporteHttpJdk() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    public TransporteHttpJdk(HttpClient cliente) {
        this.cliente = Objects.requireNonNull(cliente, "cliente");
    }

    @Override
    public RespostaHttp get(URI uri) throws IOException {
        HttpRequest pedido = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("User-Agent", "PoliticaAberta/0.1")
                .GET()
                .build();
        try {
            HttpResponse<String> resposta = cliente.send(pedido, HttpResponse.BodyHandlers.ofString());
            return new RespostaHttp(resposta.statusCode(), resposta.headers().map(), resposta.body());
        } catch (InterruptedException erro) {
            Thread.currentThread().interrupt();
            throw new IOException("coleta interrompida", erro);
        }
    }
}
