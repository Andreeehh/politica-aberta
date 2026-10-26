package org.politicaaberta.parlamentares.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.politicaaberta.parlamentares.domain.Deputado;
import org.politicaaberta.parlamentares.domain.DeputadoRepositorio;
import org.politicaaberta.parlamentares.domain.FiltroDeputados;
import org.politicaaberta.parlamentares.domain.RecorteDeputados;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

public class DeputadoRepositorioJdbc implements DeputadoRepositorio {

    static final String UPSERT = """
            INSERT INTO deputado (
                id_camara, uri, nome, sigla_partido, sigla_uf,
                id_legislatura, url_foto, email, atualizado_em
            ) VALUES (
                :idCamara, :uri, :nome, :siglaPartido, :siglaUf,
                :idLegislatura, :urlFoto, :email, :atualizadoEm
            )
            ON CONFLICT (id_camara) DO UPDATE SET
                uri = EXCLUDED.uri,
                nome = EXCLUDED.nome,
                sigla_partido = EXCLUDED.sigla_partido,
                sigla_uf = EXCLUDED.sigla_uf,
                id_legislatura = EXCLUDED.id_legislatura,
                url_foto = EXCLUDED.url_foto,
                email = EXCLUDED.email,
                atualizado_em = EXCLUDED.atualizado_em
            """;

    static final String COLUNAS = """
            id_camara, uri, nome, sigla_partido, sigla_uf,
            id_legislatura, url_foto, email, atualizado_em
            """;

    static final String FILTRO = """
            WHERE (CAST(:siglaUf AS text) IS NULL OR upper(sigla_uf) = upper(CAST(:siglaUf AS text)))
              AND (CAST(:siglaPartido AS text) IS NULL OR upper(sigla_partido) = upper(CAST(:siglaPartido AS text)))
            """;

    private static final RowMapper<Deputado> LINHA = (rs, numero) -> new Deputado(
            rs.getLong("id_camara"),
            rs.getString("uri"),
            rs.getString("nome"),
            rs.getString("sigla_partido"),
            rs.getString("sigla_uf"),
            rs.getObject("id_legislatura", Integer.class),
            rs.getString("url_foto"),
            rs.getString("email"),
            instante(rs));

    private final JdbcClient jdbc;

    public DeputadoRepositorioJdbc(JdbcClient jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    @Override
    public void salvar(Deputado deputado) {
        Objects.requireNonNull(deputado, "deputado");
        jdbc.sql(UPSERT)
                .param("idCamara", deputado.idCamara())
                .param("uri", deputado.uri())
                .param("nome", deputado.nome())
                .param("siglaPartido", deputado.siglaPartido())
                .param("siglaUf", deputado.siglaUf())
                .param("idLegislatura", deputado.idLegislatura())
                .param("urlFoto", deputado.urlFoto())
                .param("email", deputado.email())
                .param("atualizadoEm", OffsetDateTime.ofInstant(deputado.atualizadoEm(), ZoneOffset.UTC))
                .update();
    }

    @Override
    public Optional<Deputado> buscarPorIdCamara(long idCamara) {
        return jdbc.sql("SELECT " + COLUNAS + " FROM deputado WHERE id_camara = :idCamara")
                .param("idCamara", idCamara)
                .query(LINHA)
                .optional();
    }

    @Override
    public List<Deputado> listar() {
        return List.copyOf(jdbc.sql("SELECT " + COLUNAS + " FROM deputado ORDER BY id_camara")
                .query(LINHA)
                .list());
    }

    @Override
    public RecorteDeputados consultar(FiltroDeputados filtro, int deslocamento, int limite) {
        Objects.requireNonNull(filtro, "filtro");
        if (deslocamento < 0 || limite < 1) {
            throw new IllegalArgumentException("recorte invalido");
        }
        Long total = jdbc.sql("SELECT count(*) FROM deputado " + FILTRO)
                .param("siglaUf", filtro.siglaUf())
                .param("siglaPartido", filtro.siglaPartido())
                .query(Long.class)
                .single();
        List<Deputado> pagina = jdbc.sql(
                        "SELECT " + COLUNAS + " FROM deputado " + FILTRO
                                + " ORDER BY id_camara LIMIT :limite OFFSET :deslocamento")
                .param("siglaUf", filtro.siglaUf())
                .param("siglaPartido", filtro.siglaPartido())
                .param("limite", limite)
                .param("deslocamento", deslocamento)
                .query(LINHA)
                .list();
        return new RecorteDeputados(pagina, total == null ? 0 : total);
    }

    private static Instant instante(ResultSet rs) throws SQLException {
        OffsetDateTime offset = rs.getObject("atualizado_em", OffsetDateTime.class);
        if (offset != null) {
            return offset.toInstant();
        }
        Timestamp timestamp = rs.getTimestamp("atualizado_em");
        if (timestamp == null) {
            throw new IllegalStateException("atualizado_em nulo em deputado " + rs.getLong("id_camara"));
        }
        return timestamp.toInstant();
    }
}
