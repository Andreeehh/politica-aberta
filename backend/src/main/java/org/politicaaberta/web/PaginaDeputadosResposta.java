package org.politicaaberta.web;

import java.util.List;

public record PaginaDeputadosResposta(
        int pagina,
        int tamanho,
        long total,
        List<DeputadoResposta> dados
) {
}
