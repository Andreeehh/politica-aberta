CREATE TABLE execucao_importacao (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo TEXT NOT NULL,
    iniciada_em TIMESTAMPTZ NOT NULL,
    terminada_em TIMESTAMPTZ,
    status TEXT NOT NULL,
    quantidade INTEGER NOT NULL,
    mensagem_erro TEXT,
    CONSTRAINT execucao_importacao_status
        CHECK (status IN ('em_execucao', 'concluida', 'falhou')),
    CONSTRAINT execucao_importacao_quantidade
        CHECK (quantidade >= 0),
    CONSTRAINT execucao_importacao_coerente CHECK (
        (status = 'em_execucao' AND terminada_em IS NULL AND mensagem_erro IS NULL)
        OR (status = 'concluida' AND terminada_em IS NOT NULL AND mensagem_erro IS NULL)
        OR (status = 'falhou' AND terminada_em IS NOT NULL AND mensagem_erro IS NOT NULL)
    )
);

CREATE UNIQUE INDEX execucao_importacao_uma_ativa
    ON execucao_importacao (tipo)
    WHERE status = 'em_execucao';

COMMENT ON TABLE execucao_importacao IS
    'Historico das importacoes explicitas. Nao guarda corpo HTTP nem credencial.';
COMMENT ON COLUMN execucao_importacao.quantidade IS
    'Deputados gravados nesta execucao. Zero enquanto ela segue em andamento.';
