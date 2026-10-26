package org.politicaaberta.web;

import org.politicaaberta.parlamentares.domain.DeputadoRepositorio;
import org.politicaaberta.parlamentares.domain.FiltroDeputados;
import org.politicaaberta.parlamentares.domain.RecorteDeputados;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta os deputados ja gravados. Nao chama a Camara.
 */
@RestController
@RequestMapping("/api/deputados")
@ConditionalOnProperty(prefix = "politica-aberta", name = "banco", havingValue = "true", matchIfMissing = true)
public class DeputadoController {

    static final int TAMANHO_PADRAO = 20;
    static final int TAMANHO_MAXIMO = 100;
    static final int PAGINA_MAXIMA = 10_000;

    private final DeputadoRepositorio deputados;

    public DeputadoController(DeputadoRepositorio deputados) {
        this.deputados = deputados;
    }

    @GetMapping
    public PaginaDeputadosResposta listar(
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "" + TAMANHO_PADRAO) int tamanho,
            @RequestParam(required = false) String siglaUf,
            @RequestParam(required = false) String siglaPartido
    ) {
        validarPagina(pagina, tamanho);
        RecorteDeputados recorte = deputados.consultar(
                new FiltroDeputados(textoOpcional(siglaUf, "siglaUf"), textoOpcional(siglaPartido, "siglaPartido")),
                (pagina - 1) * tamanho,
                tamanho);
        return new PaginaDeputadosResposta(
                pagina,
                tamanho,
                recorte.total(),
                recorte.deputados().stream().map(DeputadoResposta::de).toList());
    }

    @GetMapping("/{id}")
    public DeputadoResposta buscar(@PathVariable long id) {
        if (id < 1) {
            throw new RequisicaoInvalida("id deve ser positivo.");
        }
        return deputados.buscarPorIdCamara(id)
                .map(DeputadoResposta::de)
                .orElseThrow(() -> new DeputadoNaoEncontrado(id));
    }

    private static void validarPagina(int pagina, int tamanho) {
        if (pagina < 1 || pagina > PAGINA_MAXIMA) {
            throw new RequisicaoInvalida("pagina deve estar entre 1 e " + PAGINA_MAXIMA + ".");
        }
        if (tamanho < 1 || tamanho > TAMANHO_MAXIMO) {
            throw new RequisicaoInvalida("tamanho deve estar entre 1 e " + TAMANHO_MAXIMO + ".");
        }
    }

    private static String textoOpcional(String valor, String nome) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim();
        if (limpo.isEmpty() || limpo.length() > 32) {
            throw new RequisicaoInvalida(nome + " invalida.");
        }
        return limpo;
    }
}
