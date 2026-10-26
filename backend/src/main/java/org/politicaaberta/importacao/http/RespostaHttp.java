package org.politicaaberta.importacao.http;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Resposta bruta de um GET. O cliente de deputados traduz isto
 * para o resultado publico e nao devolve este tipo.
 */
public record RespostaHttp(int status, Map<String, List<String>> cabecalhos, String corpo) {

    public RespostaHttp {
        cabecalhos = cabecalhos == null ? Map.of() : Map.copyOf(cabecalhos);
        corpo = corpo == null ? "" : corpo;
    }

    public String primeiroCabecalho(String nome) {
        Objects.requireNonNull(nome, "nome");
        String procurado = nome.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, List<String>> entrada : cabecalhos.entrySet()) {
            if (entrada.getKey() != null && entrada.getKey().toLowerCase(Locale.ROOT).equals(procurado)) {
                for (String valor : entrada.getValue()) {
                    if (valor != null && !valor.isBlank()) {
                        return valor.trim();
                    }
                }
            }
        }
        return null;
    }
}
