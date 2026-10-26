package org.politicaaberta.importacao.camara;

import org.politicaaberta.importacao.http.TransporteHttp;
import org.politicaaberta.importacao.http.TransporteHttpJdk;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Liga o cliente ja testado ao contexto. Nao altera a paginacao
 * e nao dispara coleta na subida.
 */
@Configuration
public class ConfiguracaoClienteCamara {

    @Bean
    TransporteHttp transporteHttpCamara() {
        return new TransporteHttpJdk();
    }

    @Bean
    ClienteDeputadosCamara clienteDeputadosCamara(TransporteHttp transporteHttpCamara) {
        return new ClienteDeputadosCamara(transporteHttpCamara);
    }
}
