package org.politicaaberta.importacao.http;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransporteHttpJdkTest {

    @Test
    void enviaAcceptELeCabecalhoSemSairDaMaquina() throws Exception {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/deputados", troca -> {
            String accept = troca.getRequestHeaders().getFirst("Accept");
            byte[] corpo = accept == null
                    ? new byte[0]
                    : "{\"dados\":[]}".getBytes(StandardCharsets.UTF_8);
            int status = "application/json".equals(accept) ? 200 : 415;
            troca.getResponseHeaders().add("X-Total-Count", "8");
            troca.sendResponseHeaders(status, corpo.length);
            troca.getResponseBody().write(corpo);
            troca.close();
        });
        servidor.start();
        try {
            int porta = servidor.getAddress().getPort();
            URI uri = URI.create("http://127.0.0.1:" + porta + "/deputados");
            RespostaHttp resposta = new TransporteHttpJdk().get(uri);

            assertEquals(200, resposta.status());
            assertEquals("8", resposta.primeiroCabecalho("x-total-count"));
            assertTrue(resposta.corpo().contains("dados"));
        } finally {
            servidor.stop(0);
        }
    }
}
