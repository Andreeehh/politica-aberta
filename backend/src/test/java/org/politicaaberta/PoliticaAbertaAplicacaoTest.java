package org.politicaaberta;

import org.junit.jupiter.api.Test;
import org.politicaaberta.importacao.camara.ClienteDeputadosCamara;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

// A suite padrao nao abre PostgreSQL. A aplicacao real exige o banco.
@SpringBootTest(properties = {
        "politica-aberta.banco=false",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class PoliticaAbertaAplicacaoTest {

    @Autowired
    private ClienteDeputadosCamara cliente;

    @Autowired
    private ObjectProvider<DataSource> fontesDeDados;

    @Test
    void contextoSobeComClienteESemBanco() {
        assertNotNull(cliente);
        assertNull(fontesDeDados.getIfAvailable());
    }
}
