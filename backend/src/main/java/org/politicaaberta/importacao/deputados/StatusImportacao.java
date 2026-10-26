package org.politicaaberta.importacao.deputados;

public enum StatusImportacao {
    EM_EXECUCAO("em_execucao"),
    CONCLUIDA("concluida"),
    FALHOU("falhou");

    private final String codigo;

    StatusImportacao(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static StatusImportacao porCodigo(String codigo) {
        for (StatusImportacao status : values()) {
            if (status.codigo.equals(codigo)) {
                return status;
            }
        }
        throw new IllegalArgumentException("status de importacao desconhecido: " + codigo);
    }
}
