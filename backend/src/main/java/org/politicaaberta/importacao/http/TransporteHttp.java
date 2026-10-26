package org.politicaaberta.importacao.http;

import java.io.IOException;
import java.net.URI;

/**
 * Porta de GET. A implementacao de producao e o cliente do JDK;
 * o teste injeta uma resposta gravada e nao abre rede.
 */
public interface TransporteHttp {

    RespostaHttp get(URI uri) throws IOException;
}
