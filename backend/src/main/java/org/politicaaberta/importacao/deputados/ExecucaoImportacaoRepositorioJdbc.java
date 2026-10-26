package org.politicaaberta.importacao.deputados;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;

public class ExecucaoImportacaoRepositorioJdbc implements ExecucaoImportacaoRepositorio {

    private final JdbcClient jdbc;

    public ExecucaoImportacaoRepositorioJdbc(JdbcClient jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    @Override
    public ExecucaoImportacao iniciar(String tipo, Instant inicio) {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(inicio, "inicio");
        try {
            long id = jdbc.sql("""
                            INSERT INTO execucao_importacao (tipo, iniciada_em, status, quantidade)
                            VALUES (:tipo, :iniciadaEm, 'em_execucao', 0)
                            RETURNING id
                            """)
                    .param("tipo", tipo)
                    .param("iniciadaEm", emUtc(inicio))
                    .query(Long.class)
                    .single();
            return new ExecucaoImportacao(id, tipo, inicio, null, StatusImportacao.EM_EXECUCAO, 0, null);
        } catch (DuplicateKeyException erro) {
            throw new ImportacaoEmAndamento(tipo);
        }
    }

    @Override
    public ExecucaoImportacao concluir(long id, int quantidade, Instant termino) {
        int linhas = jdbc.sql("""
                        UPDATE execucao_importacao
                        SET status = 'concluida',
                            terminada_em = :termino,
                            quantidade = :quantidade
                        WHERE id = :id AND status = 'em_execucao'
                        """)
                .param("id", id)
                .param("quantidade", quantidade)
                .param("termino", emUtc(termino))
                .update();
        if (linhas != 1) {
            throw new IllegalStateException("execucao " + id + " nao estava em andamento");
        }
        return buscar(id);
    }

    @Override
    public ExecucaoImportacao falhar(long id, int quantidade, Instant termino, String mensagem) {
        String texto = mensagem == null || mensagem.isBlank() ? "falha sem detalhe" : mensagem;
        int linhas = jdbc.sql("""
                        UPDATE execucao_importacao
                        SET status = 'falhou',
                            terminada_em = :termino,
                            quantidade = :quantidade,
                            mensagem_erro = :mensagem
                        WHERE id = :id AND status = 'em_execucao'
                        """)
                .param("id", id)
                .param("quantidade", quantidade)
                .param("termino", emUtc(termino))
                .param("mensagem", texto)
                .update();
        if (linhas != 1) {
            throw new IllegalStateException("execucao " + id + " nao estava em andamento");
        }
        return buscar(id);
    }

    private ExecucaoImportacao buscar(long id) {
        return jdbc.sql("""
                        SELECT id, tipo, iniciada_em, terminada_em, status, quantidade, mensagem_erro
                        FROM execucao_importacao
                        WHERE id = :id
                        """)
                .param("id", id)
                .query((rs, numero) -> new ExecucaoImportacao(
                        rs.getLong("id"),
                        rs.getString("tipo"),
                        instante(rs.getObject("iniciada_em", OffsetDateTime.class)),
                        instante(rs.getObject("terminada_em", OffsetDateTime.class)),
                        StatusImportacao.porCodigo(rs.getString("status")),
                        rs.getInt("quantidade"),
                        rs.getString("mensagem_erro")))
                .single();
    }

    private static OffsetDateTime emUtc(Instant instante) {
        return OffsetDateTime.ofInstant(Objects.requireNonNull(instante, "instante"), ZoneOffset.UTC);
    }

    private static Instant instante(OffsetDateTime valor) {
        return valor == null ? null : valor.toInstant();
    }
}
