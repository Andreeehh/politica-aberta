-- Espelho da lista GET /deputados. Nao ha nome civil separado, nome
-- parlamentar, CPF nem JSON da resposta.

CREATE TABLE deputado (
    id_camara BIGINT PRIMARY KEY,
    uri TEXT,
    nome TEXT,
    sigla_partido TEXT,
    sigla_uf TEXT,
    id_legislatura INTEGER,
    url_foto TEXT,
    email TEXT,
    atualizado_em TIMESTAMPTZ NOT NULL
);

COMMENT ON TABLE deputado IS
    'Deputado federal gravado a partir da lista oficial. O id da Camara e a chave.';
COMMENT ON COLUMN deputado.id_camara IS
    'id de GET /deputados, o mesmo ideCadastro da cota. Unico e estavel.';
COMMENT ON COLUMN deputado.nome IS
    'Campo nome da lista. A lista nao separa nome civil e nome parlamentar.';
COMMENT ON COLUMN deputado.uri IS
    'URL oficial do deputado na API da Camara.';
COMMENT ON COLUMN deputado.email IS
    'E-mail publico ja presente na lista. Nao e CPF.';
COMMENT ON COLUMN deputado.atualizado_em IS
    'Instante local da ultima gravacao deste registro.';
