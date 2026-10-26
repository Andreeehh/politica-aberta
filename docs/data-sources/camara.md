# Fonte de dados: Câmara dos Deputados

Verificação em **2026-10-26**. A API e os arquivos descritos abaixo foram lidos na documentação oficial e, quando indicado, em respostas reais. Nada fora dessas duas origens deve ser tratado como garantido.

Como ler cada afirmação:

- **Documentado:** texto do OpenAPI ou do portal de Dados Abertos.
- **Observado:** resposta ou arquivo obtido nesta verificação.
- **Desconhecido:** não há evidência suficiente. Não implementar em cima disso.

A decisão de importar para o PostgreSQL em tarefa agendada continua a do [ADR 0002](../decisions/0002-estrategia-de-importacao.md). Este documento fecha as pendências de fonte. Não altera a arquitetura.

## 1. Serviço

| Item | Valor |
|---|---|
| Portal | https://dadosabertos.camara.leg.br |
| Swagger UI | https://dadosabertos.camara.leg.br/swagger/api.html |
| OpenAPI | https://dadosabertos.camara.leg.br/api/v2/api-docs |
| Base das chamadas | `https://dadosabertos.camara.leg.br/api/v2` |
| Especificação obtida | OpenAPI 3.0.1, `info.version` `v1.0`, texto interno **0.4.347 - 10/26/2026 10:26 AM** |
| Operações no OpenAPI | 78, todas `GET` |
| Formatos | JSON e XML. Pedir `Accept: application/json` |

**Documentado:** a versão é incompleta, sujeita a mudança e não substitui o portal antigo (http://www2.camara.leg.br/transparencia/dados-abertos/dados-abertos-legislativo, na descrição oficial). Há `GET` e `HEAD`. Não há política de depreciação, SLA nem janela de manutenção no OpenAPI.

**Observado:** `GET /api/v2/` (sem recurso) responde `405` com `application/problem+json` (`type`, `title`, `status`, `detail`, `instance`). Esse corpo é diferente do erro de negócio dos recursos. O recurso precisa ser nomeado.

**Desconhecido:** licença e exigência de atribuição. Não há campo `license` no OpenAPI nem termo de uso no HTML do Swagger. O produto deve linkar a URL oficial de cada registro por regra própria, sem afirmar uma licença que a Câmara não publicou neste material.

**Desconhecido:** cota numérica de requisições. O cabeçalho `retry-after: 30` veio em respostas `200`, `400` e `404`. Não houve `429`. Não tratar esse cabeçalho como espera obrigatória de 30 segundos: a documentação não diz isso, e aplicá-lo a cada `200` tornaria a carga inviável sem base oficial.

## 2. Convenções para o cliente

### Envelope

Listas e detalhes usam:

```json
{
  "dados": [],
  "links": [{ "rel": "self", "href": "https://dadosabertos.camara.leg.br/api/v2/..." }]
}
```

`dados` é array na lista e objeto no detalhe. `links[].rel` observados: `self`, `first`, `next`, `last`, `previous`.

O mesmo `Link` aparece no cabeçalho HTTP. `X-Total-Count` traz o total da consulta nas listagens paginadas.

### Paginação

**Documentado** na introdução do OpenAPI: omissão de `itens` devolve 15; o teto geral é 100. `pagina` começa em 1. Em `GET /votacoes`, o próprio texto do endpoint diz que o padrão e o máximo são 100, e que valor maior é ignorado.

Parâmetros comuns, quando o endpoint os declara: `pagina`, `itens`, `ordem` (`asc` ou `desc`), `ordenarPor`. O default de `ordenarPor` muda por endpoint. Enviar `itens` ou `ordenarPor` a um sub-recurso que não os declara produz `400`.

**Observado** em `GET /deputados` (total 513):

| Pedido | Resultado |
|---|---|
| `itens=2&ordem=ASC&ordenarPor=nome` | página 1: Acácio Favacho (`204379`), Adail Filho (`220714`); `Link` com `last` na página 257 |
| página 2, mesmos parâmetros | Adilson Barroso (`221328`), Adolfo Viana (`204560`). Sem repetir a página 1 |
| página 257 | um registro (Zucco, `220552`); há `previous` e não há `next`. 256×2+1 = 513 |
| página 258 | `200`, `dados: []`, `X-Total-Count: 0`, só `rel=self` |
| `itens=101` | 101 registros. O teto documentado de 100 não foi aplicado |
| `itens=200` | 200 registros |

`X-Total-Count: 0` na página além do fim **não** é o total da coleção. Guardar o total da primeira página que trouxer dado.

**Recomendação do cliente:** pedir `itens=100`, mesmo o servidor tendo aceitado mais em `/deputados`. Percorrer enquanto existir `rel=next` e `dados` não estiver vazio. Upsert por identificador torna repetir uma página seguro.

Sub-recursos sem `pagina` no OpenAPI vieram numa resposta só, apenas com `rel=self`: histórico do deputado, autores, tramitações, votos, orientações e deputados de um evento. Não paginar esses URLs.

### Erros observados

| HTTP | Quando | Corpo |
|---|---|---|
| 400 | parâmetro inválido ou não suportado | `status`, `title` "Requisição inválida", `detail` "Parâmetro(s) inválido(s).", `instance` com o nome do parâmetro, `code` 5. Cabeçalho `x-business-error: true` |
| 404 | id inexistente (`/deputados/999999999`) | `title` "Recurso não encontrado", `code` 6, `x-business-error: true` |
| 405 | `GET /api/v2/` | `application/problem+json`, sem `code` |
| 200 vazio | filtro sem resultado, ou página além do fim | `dados: []`. Não é erro |

Não repetir `400`. `404` em um id: registrar e seguir. Para `408`, `429`, `500`, `502`, `503` e `504`: retentativa com espera crescente e limite de tentativas. `429` não foi visto; se aparecer, respeitar `Retry-After`.

### Cache observado

`cache-control: public, max-age=1800` na lista de deputados; `max-age=3600` no detalhe; `max-age=360` na lista de proposições. Votações da amostra não trouxeram `cache-control`. A importação diária não precisa ser mais frequente do que isso para deputados.

### O que não existe

Não há, nos endpoints do MVP, parâmetro de "alterados desde". `dataInicio` e `dataFim` filtram ocorrência ou tramitação, não a data de edição do registro. Não há arquivo delta. Correção retroativa só aparece numa nova leitura da mesma janela ou do mesmo arquivo consolidado.

## 3. Identificadores e ligações confirmadas

| Entidade | Chave estável | Tipo | Como se liga |
|---|---|---|---|
| Deputado | `id` numérico | inteiro | Igual a `ideCadastro` no CSV da cota. Diferente de `nuDeputadoId` do mesmo arquivo |
| Partido | `id` numérico na URI `/partidos/{id}` | inteiro | A sigla não é chave. A documentação avisa que a mesma sigla pode ser de partidos distintos em legislaturas distintas |
| Legislatura | `id` sequencial | inteiro | 57 = 2023-02-01 a 2027-01-31, observado |
| Proposição | `id` numérico | inteiro | `siglaTipo` + `numero` + `ano` localiza, mas há proposição com `ano: 0` |
| Votação | `id` string, formato observado `{prefixo}-{sufixo}` | string | Não usar o prefixo como chave estrangeira. Buscar o detalhe |
| Evento | `id` numérico | inteiro | Votação pode trazer `idEvento` nulo |
| Despesa na API | não há `id` | — | Ver secção 7 |
| Despesa no CSV | não há coluna única | — | Ver secção 7 |

Ligação observada no mesmo conjunto de respostas, em 2026-10-26:

1. `GET /deputados` e `GET /deputados/204379` devolvem o mesmo `id` 204379.
2. `GET /proposicoes?idDeputadoAutor=204379` devolve a proposição 2645127. `GET /proposicoes/2645127/autores` devolve `uri` `.../deputados/204379`.
3. `GET /proposicoes?siglaTipo=PL&numero=1123&ano=2025` devolve o id 2487836.
4. `GET /votacoes/2487836-52` traz `proposicoesAfetadas[].id` 2487836 e `uriEvento` do evento 83066. Na **lista**, essa mesma votação veio com `uriProposicaoObjeto: null`.
5. `GET /votacoes/2635476-23/votos` traz `deputado_.id` (exemplo 220665) no mesmo formato do id de deputado.
6. A despesa de março/2026 do deputado 204379, documento 8077914, aparece na API (`idLegislatura=57`) e no CSV (`ideCadastro=204379`, `ideDocumento=8077914`), com o mesmo valor líquido 954,30, a mesma data de emissão e o mesmo lote 2224199.

Em 20 votações de outubro/2026, 14 vieram na lista sem `uriProposicaoObjeto` e 1 sem `uriEvento`. O detalhe é obrigatório para relacionar votação e proposição. O detalhe separa `objetosPossiveis` (pode haver várias; a Câmara avisa que a proposição realmente votada pode não estar entre elas) e `proposicoesAfetadas` (efeito de tramitação). São relações diferentes. Não colapsar numa única chave.

## 4. Deputados

### Endpoints

| Método e caminho | Função | Confirmação |
|---|---|---|
| `GET /deputados` | Lista. Sem recorte de tempo, só quem está em exercício na hora do pedido | Chamado. Total 513 em 2026-10-26 |
| `GET /deputados/{id}` | Cadastro de quem já entrou em exercício alguma vez | Chamado |
| `GET /deputados/{id}/historico` | Mudanças de nome, partido, legislatura e exercício | Chamado. O OpenAPI descreve este caminho como atalho para uma API GraphQL externa, com a estrutura do antigo v1 |
| `GET /deputados/{id}/orgaos` | Comissões e cargos | Só OpenAPI. Não chamado |
| `GET /legislaturas/{id}` | Datas da legislatura | Chamado para 57 |
| `GET /partidos` | Partidos com deputado em exercício, se não houver filtro | Só OpenAPI. Ids de partido já vêm no deputado |
| `GET /referencias/deputados/codSituacao` | Situações de exercício | Só OpenAPI. Não chamado |

Filtros documentados da lista: `id`, `nome`, `idLegislatura`, `siglaUf`, `siglaPartido`, `siglaSexo`, `dataInicio`, `dataFim`, mais paginação. `ordenarPor`: `id`, `idLegislatura`, `nome`, `siglaUF`, `siglaPartido`.

### O que a lista traz

`id`, `uri`, `nome`, `siglaPartido`, `uriPartido`, `siglaUf`, `idLegislatura`, `urlFoto`, `email`.

A lista **não** traz situação, condição eleitoral, nome civil nem mandato histórico. Situação exige o detalhe ou o histórico.

### O que o detalhe traz

Além do id e do nome civil: `ultimoStatus` (nome parlamentar, partido, UF, legislatura, gabinete, `situacao`, `condicaoEleitoral`, data desse status), sexo, data de nascimento, UF e município de nascimento, escolaridade, redes, site e o campo `cpf`.

**Não persistir nem exibir `cpf`.** Também não gravar o valor em log ou fixture. É dado pessoal; a minimização vale mesmo com a Câmara publicando o campo.

**Observado** no id 204379, na mesma data: a lista traz `uriPartido` e `email`; em `ultimoStatus` os dois vieram `null`, e o e-mail estava em `ultimoStatus.gabinete.email`. Mapear cada campo do objeto que realmente o preenche. Não assumir que lista e detalhe repetem os mesmos nulos.

`ultimoStatus` observado: `situacao` "Exercício", `condicaoEleitoral` "Titular".

### Histórico

Não tem id de evento. O `id` repetido é o do deputado. Seis linhas no id 204379, sem paginação, fora de ordem cronológica. O cliente ordena por `dataHora`.

Valores observados nesse deputado: `situacao` `Exercício`, `FIM_MANDATO` e `null`; `condicaoEleitoral` `Titular` e `null`; `descricaoStatus` com posse, alteração de partido, fim de legislatura e nome/partido no início da legislatura. Houve troca PROS para MDB na legislatura 56. Duas linhas compartilham `dataHora` `2023-02-01T00:00` e se distinguem por `idLegislatura` (56 e 57).

Chave prática de espelho, não oficial: `(id do deputado, idLegislatura, dataHora, descricaoStatus)`. Duas linhas iguais nesse conjunto não foram vistas. Se a Câmara repetir a combinação, a carga precisa detectar a colisão em vez de descartar em silêncio.

Substituir o histórico daquele deputado a cada coleta bem-sucedida. A Câmara pode corrigir o passado, e a lista não é um log append-only confiável (não há id de evento).

### Arquivo consolidado

**Documentado:** `http://dadosabertos.camara.leg.br/arquivos/deputados/{formato}/deputados.{formato}`, atualização diária. Cobre quem já exerceu mandato, com URI, nome civil e primeira e última legislatura. Não foi baixado nesta verificação. A lista da API basta para o conjunto em exercício (513 pessoas, seis páginas de 100).

## 5. Proposições

### Endpoints

| Método e caminho | Função | Confirmação |
|---|---|---|
| `GET /proposicoes` | Lista. Sem outro recorte, tramitação dos últimos 30 dias | Chamado com filtros |
| `GET /proposicoes/{id}` | Detalhe, inclusive `statusProposicao` | Chamado (2645127) |
| `GET /proposicoes/{id}/autores` | Autores e apoiadores. Todo signatário é autor, segundo o texto oficial | Chamado |
| `GET /proposicoes/{id}/tramitacoes` | Passos da tramitação. O texto oficial diz que a representação é provisória | Chamado. 35 itens na proposição 2487836, numa resposta só |
| `GET /proposicoes/{id}/votacoes` | Votações que tiveram a proposição como objeto ou como afetada | Chamado. A 2645127 devolveu lista vazia. `itens` nesse caminho devolveu `400` (`instance`: `itens`) |
| `GET /proposicoes/{id}/temas` | Temas oficiais | Só OpenAPI |
| `GET /proposicoes/{id}/relacionadas` | Pareceres, requerimentos, substitutivos etc. | Só OpenAPI |
| `GET /referencias/proposicoes/siglaTipo` | Tipos | Só OpenAPI |
| `GET /referencias/proposicoes/codSituacao` | Situações | Só OpenAPI |
| `GET /referencias/proposicoes/codTema` | Temas | Só OpenAPI |

### Filtros que importam

Documentados e úteis ao MVP: `id`, `siglaTipo`, `numero`, `ano`, `idDeputadoAutor`, `autor`, `dataInicio`, `dataFim` (tramitação), `dataApresentacaoInicio`, `dataApresentacaoFim`, `codTema`, `keywords`, paginação. `ordenarPor`: `id`, `codTipo`, `siglaTipo`, `numero`, `ano`.

**Documentado:** se a busca usa `id`, `ano`, `dataApresentacaoInicio`, `dataApresentacaoFim`, `idAutor` ou `autor`, o recorte padrão de 30 dias de tramitação só entra quando `dataInicio` ou `dataFim` vão explícitos. Sem isso, a resposta pode incluir proposição antiga e demorar. O parâmetro declarado no OpenAPI é `idDeputadoAutor`, não `idAutor`; o nome `idAutor` aparece só nesse parágrafo. O teste que funcionou usou `idDeputadoAutor`. Não foi enviado `idAutor`. O texto oficial também diz que `codSituacao` pode dar resultado inesperado. Não usar `codSituacao` como filtro de sincronização.

**Observado:**

- `siglaTipo=PL&numero=1123&ano=2025` → total 1, id 2487836.
- `idDeputadoAutor=204379&ordem=DESC&ordenarPor=id` → total 635.

`dataInicio`/`dataFim` e `codTema` não foram exercitados. O comportamento padrão de 30 dias está documentado e não foi medido (volume desconhecido).

### Campos e autores

Lista: `id`, `uri`, `siglaTipo`, `codTipo`, `numero`, `ano`, `ementa`, `dataApresentacao`.

Detalhe acrescenta `statusProposicao` (data, sequência, órgão, regime, tramitação, situação, despacho, URL do teor, âmbito, apreciação), `uriAutores`, tipo por extenso, ementa detalhada, keywords, proposição principal/anterior/posterior e URL do inteiro teor. Vários desses podem vir `null`. `ano` 0 foi observado em requerimento de inversão de pauta.

Autor observado: `uri`, `nome`, `codTipo`, `tipo`, `ordemAssinatura`, `proponente`. Deputado veio como `tipo` "Deputado(a)", `codTipo` 10000 e `uri` do deputado. Autor também pode ser senador, sociedade civil, assembleia ou outro poder; nesses casos a `uri` pode apontar para outro recurso ou faltar. Seguir a `uri` só quando ela existir e for de `/deputados/{id}`.

### Tramitações

Campos observados: `dataHora`, `sequencia`, `siglaOrgao`, `uriOrgao`, `uriUltimoRelator`, `regime`, `descricaoTramitacao`, `codTipoTramitacao`, `descricaoSituacao`, `codSituacao`, `despacho`, `url`, `ambito`, `apreciacao`.

Na proposição 2487836 a `sequencia` chegou a 61 em 35 itens. Não é índice contínuo da lista. Chave prática: `(id da proposição, dataHora, sequencia)`. Substituir o conjunto inteiro da proposição a cada leitura.

O OpenAPI declara o `200` desse endpoint como esquema `ResultadoFrentes`. O corpo observado é o de tramitação. Ignorar o nome do esquema; validar pelos campos acima.

### Arquivos

**Documentado**, atualização diária, não baixados nesta verificação:

- `http://dadosabertos.camara.leg.br/arquivos/proposicoes/{formato}/proposicoes-{ano}.{formato}` — um registro por proposição do ano de apresentação, com a tramitação mais recente, não o histórico.
- `.../proposicoesAutores/{formato}/proposicoesAutores-{ano}.{formato}`
- `.../proposicoesTemas/{formato}/proposicoesTemas-{ano}.{formato}`

O portal informa cobertura parcial em 1934–1945, tipos que podiam virar norma entre 1946 e 2000, e todas as proposições a partir de 2001. PECs de 1967 que só tramitaram em conjunto no Congresso não entram.

O arquivo anual não substitui `GET /proposicoes/{id}/tramitacoes` quando o produto precisa do histórico de passos.

## 6. Votações

### Endpoints

| Método e caminho | Função | Confirmação |
|---|---|---|
| `GET /votacoes` | Lista. Sem data, últimos 30 dias, todos os órgãos | Chamado com datas |
| `GET /votacoes/{id}` | Detalhe, objetos possíveis e proposições afetadas | Chamado. `{id}` é string |
| `GET /votacoes/{id}/votos` | Voto nominal aberto. Ausente não aparece. Simbólica tende a lista vazia; há simbólica com registro avulso de quem pediu para constar | Chamado |
| `GET /votacoes/{id}/orientacoes` | Orientação de bancada. Documentado: só Plenário até o momento | Chamado |
| `GET /proposicoes/{id}/votacoes` | Atalho pela proposição | Chamado |
| `GET /eventos/{id}/votacoes` | Votações do evento | Só OpenAPI |

Filtros da lista: `id`, `idProposicao`, `idEvento`, `idOrgao`, `dataInicio`, `dataFim`, paginação. `ordenarPor`: `id`, `idOrgao`, `siglaOrgao`, `idEvento`, `idProposicao`, `data`, `dataHoraRegistro`, `idProposicaoObjeto`.

**Documentado:** `dataInicio` e `dataFim` precisam ser do mesmo ano. Uma data sozinha cobre o restante daquele ano. O máximo de `itens` neste endpoint é 100.

**Observado:**

- `dataInicio=2026-10-01&dataFim=2026-10-26` → total 78. `itens=20` devolveu 20.
- `dataInicio=2025-12-01&dataFim=2026-01-15` → `200`, não `400`, total 1585. A mais antiga ordenada por `data` foi 2025-12-01; a mais recente, 2025-12-23. Nenhuma linha de 2026 nessa amostra de extremos.
- `dataInicio=2026-01-01&dataFim=2026-01-15` → total 0.

Não dá para afirmar que o servidor cortou 2026 na consulta cruzada: a quinzena de janeiro/2026 também veio vazia. Também não dá para afirmar que o cruzamento de ano seja seguro. O cliente deve partir o intervalo em anos civis, como o texto oficial manda.

Lista: `id`, `uri`, `data`, `dataHoraRegistro`, `siglaOrgao`, `uriOrgao`, `uriEvento`, `proposicaoObjeto`, `uriProposicaoObjeto`, `descricao`, `aprovacao` (1 ou 0 na amostra). `dataHoraRegistro` e `uriEvento` podem ser `null`.

Não há campo de modalidade (nominal, simbólica, secreta). A distinção observada é indireta: a votação `2487836-52` devolveu `votos` vazio; a `2635476-23` devolveu 399 votos. Isso confirma o texto oficial, não cria uma tipologia completa.

### Votos

Campos: `tipoVoto`, `dataRegistroVoto`, `deputado_` com o mesmo bloco básico do deputado (`id`, `uri`, `nome`, partido, UF, legislatura, foto, e-mail).

Na votação `2635476-23` (Plenário, 2026-10-14): 387 `Não`, 11 `Sim`, 1 `Artigo 17`. A descrição da votação diz "Sim: 11; Não: 387; Total: 398". O total escrito não inclui a linha `Artigo 17`. Não inventar o efeito regimental desse rótulo. Guardar o texto oficial do voto.

Não apareceram, nesta votação, abstenção, obstrução nem lista de ausentes. Ausência é falta de linha, não um `tipoVoto`. Votação secreta não foi encontrada nesta amostra; o OpenAPI diz que o endpoint é para nominal que não tenha sido secreta.

Chave de espelho do voto: `(id da votação, id do deputado)`. Substituir o conjunto de votos daquela votação a cada leitura, porque um registro simbólico avulso pode surgir depois e uma correção pode retirar linha.

### Orientações

Campos: `orientacaoVoto`, `codTipoLideranca`, `siglaPartidoBloco`, `codPartidoBloco`, `uriPartidoBloco`. Na mesma votação de plenário, 15 linhas. `orientacaoVoto` observado: `Não`, `Sim` e string vazia. `codTipoLideranca` `P` e `B`. Bancada sem `uriPartidoBloco` (bloco e liderança como Maioria) não liga a `/partidos/{id}`.

Obstrução, "liberado" e outros rótulos estão descritos no texto oficial e não vieram nesta amostra. Persistir o texto cru.

### Arquivos

**Documentado**, por ano da votação, atualização diária, não baixados aqui:

- `http://dadosabertos.camara.leg.br/arquivos/votacoes/{formato}/votacoes-{ano}.{formato}`
- `.../votacoesVotos/.../votacoesVotos-{ano}.{formato}`
- `.../votacoesOrientacoes/...`
- `.../votacoesObjetos/...` — objetos possíveis, não o objeto confirmado
- `.../votacoesProposicoes/...` — proposições afetadas

Servem para a carga histórica. A atualização corrente cabe na API, em janela dentro de um único ano. Validar o cabeçalho do arquivo na implementação, antes de mapear coluna: esta verificação não abriu esses ZIPs.

## 7. Despesas da cota parlamentar

Há duas fontes com esquemas diferentes. O portal diz que os arquivos da cota **não seguem** a nomenclatura dos outros arquivos. Tutorial oficial dos campos: https://dadosabertos.camara.leg.br/howtouse/2023-12-26-dados-ceap.html (não relido campo a campo nesta tarefa; o CSV de 2026 foi lido direto).

### API

`GET /deputados/{id}/despesas`

**Documentado:** sem recorte de tempo, devolve os seis meses anteriores ao pedido. Filtros: `idLegislatura`, `ano`, `mes`, `cnpjCpfFornecedor`, paginação. `ordenarPor` aceita qualquer campo do retorno e também `idLegislatura`.

Campos do esquema: `ano`, `mes`, `tipoDespesa`, `codDocumento`, `tipoDocumento`, `codTipoDocumento`, `dataDocumento`, `numDocumento`, `valorDocumento`, `urlDocumento`, `nomeFornecedor`, `cnpjCpfFornecedor`, `valorLiquido`, `valorGlosa`, `numRessarcimento`, `codLote`, `parcela`. Não há `id`.

**Observado** no deputado 204379:

| Consulta | `X-Total-Count` |
|---|---|
| sem data | 0 |
| `ano=2024` | 0 |
| `ano=2025&mes=12` | 0 |
| `ano=2026` | 0 |
| `ano=2026&mes=3` | 0 |
| `mes=3` | 0 |
| `idLegislatura=57` | 1245, incluindo documento de 2026 |
| `idLegislatura=57&ano=2026` | 186 |

No deputado 220665, `ano=2023` sem legislatura também devolveu 0. No deputado 220714, `ano=2025` sem legislatura devolveu 0.

O filtro `ano` **sozinho** não reproduziu a documentação nesta data. Com `idLegislatura`, devolveu despesa. A causa não foi investigada além desses pedidos. O cliente não deve sincronizar despesa pela API usando só `ano` ou só `mes`.

O documento 8077914 (março/2026, valor líquido 954,30) veio na API com `idLegislatura=57` e no CSV. A API deu `urlDocumento` em `http://`; o CSV, em `https://`. É o mesmo `ideDocumentoFiscal`.

`cnpjCpfFornecedor` pode ser CPF de pessoa física. Não exibir CPF de fornecedor. Não usar o CPF do deputado, que esta rota nem precisa devolver.

Volume pela API, se alguém ainda for por ela: 1245 lançamentos numa legislatura de um deputado. Com `itens=100`, cerca de 13 páginas por deputado. Para centenas de deputados, a conta passa de vários milhares de pedidos por carga. O arquivo anual evita isso.

### Arquivo anual

**Documentado:** um arquivo por ano desde 2008, atualização diária.

`https://www.camara.leg.br/cotas/Ano-{ano}.csv.zip`

Também `.json.zip`, `.xml.zip`, `.xlsx` e `.ods`. O HTML do portal mostra o padrão com `http://www.camara.leg.br/cotas/...`; o ZIP de 2026 foi baixado pelo `https` correspondente.

**Observado** em `Ano-2026.csv.zip` (baixado em 2026-10-26, ZIP de cerca de 4,3 MB, CSV de 45.140.167 bytes, UTF-8 com BOM, separador `;`):

- 118.559 linhas, 32 colunas.
- 565 `ideCadastro` distintos. 547 linhas com `ideCadastro` vazio, entre elas cota de liderança, com `nuDeputadoId` preenchido. Essas linhas não ligam a `GET /deputados/{id}`.
- Meses 1 a 10 com dados. Novembro e dezembro ainda não aparecem, coerente com a data do arquivo, não com uma regra.
- 563 linhas com `vlrLiquido` negativo.
- `ideDocumento` não é único. O valor `0` cobre 2.237 linhas. Outros 46 documentos repetem com líquido diferente, em geral o par despesa e restituição (`vlrLiquido` 0 e `vlrRestituicao` preenchido na segunda linha). Cinco linhas têm `ideDocumento` vazio.
- A linha inteira, excluída a coluna `cpf`, é única nas 118.559 linhas. Incluir `cpf` na chave é desnecessário e indesejável.
- `ideCadastro` 204379 tem 147 linhas no arquivo. A API, com legislatura 57 e `ano=2026`, informou total 186. Os dois números não foram reconciliados linha a linha. Um documento (8077914) confere. Não tratar as duas fontes como espelhos perfeitos.

Colunas: `txNomeParlamentar`, `cpf`, `ideCadastro`, `nuCarteiraParlamentar`, `nuLegislatura`, `sgUF`, `sgPartido`, `codLegislatura`, `numSubCota`, `txtDescricao`, `numEspecificacaoSubCota`, `txtDescricaoEspecificacao`, `txtFornecedor`, `txtCNPJCPF`, `txtNumero`, `indTipoDocumento`, `datEmissao`, `vlrDocumento`, `vlrGlosa`, `vlrLiquido`, `numMes`, `numAno`, `numParcela`, `txtPassageiro`, `txtTrecho`, `numLote`, `numRessarcimento`, `datPagamentoRestituicao`, `vlrRestituicao`, `nuDeputadoId`, `ideDocumento`, `urlDocumento`.

Não gravar `cpf`. `txtCNPJCPF` pode ser CPF; não publicar quando for CPF. `txtPassageiro` é dado de viagem de pessoa; não exibir no MVP.

Chave de idempotência recomendada: hash estável de todas as colunas exceto `cpf`, mais o ano do arquivo. Não há id oficial. Descartar "duplicata" só por `ideDocumento` apaga restituição e telefonia lançada com documento `0`.

## 8. Eventos e presença

| Método e caminho | Função | Confirmação |
|---|---|---|
| `GET /eventos` | Eventos. Sem data, cinco dias antes, o dia e cinco dias depois | Chamado com datas. `dataInicio=2026-10-20&dataFim=2026-10-26` → total 3 |
| `GET /eventos/{id}` | Detalhe | Só OpenAPI |
| `GET /eventos/{id}/deputados` | Se o evento já ocorreu, quem registrou presença. Se ainda não ocorreu, quem é esperado (membro ou convidado) | Chamado no evento 83047 |
| `GET /deputados/{id}/eventos` | Eventos em que a participação do deputado era ou é prevista. Sem data, cinco dias ao redor do pedido | Chamado. Outubro/2026 do deputado 204379 → total 3 |
| `GET /eventos/{id}/votacoes` | Votações do evento, se deliberativo | Só OpenAPI |

O payload de `/eventos/{id}/deputados` é o bloco básico do deputado. Não há campo "presente", hora de registro nem ausência. No evento 83047, sessão deliberativa já encerrada, vieram 491 deputados numa resposta só.

Presença no evento, nome na lista de um evento futuro e voto em plenário são fatos diferentes. O voto não lista ausente. Um indicador público de "presença" ou "comparecimento às votações" misturaria essas fontes e superestimaria quem apenas estava convocado. Fora do MVP. Não transformar isso em taxa sem uma decisão própria, com a regra escrita ao lado do número.

**Documentado** e não baixado: `http://dadosabertos.camara.leg.br/arquivos/eventosPresencaDeputados/{formato}/eventosPresencaDeputados-{ano}.{formato}`, com a mesma ressalva de eventos futuros. Atualização diária.

## 9. Estratégia de sincronização

Princípios já decididos e agora aplicáveis com o que a fonte realmente oferece:

- Upsert pela chave da secção 3. Repetir a carga não cria outro deputado, outra proposição nem outra votação.
- Gravar URL oficial, data da coleta e identificador da execução.
- Não há delta por data de alteração. Incremental significa **reler uma janela** ou **reler o arquivo do ano**.
- Falha no meio: a próxima execução recomeça a janela ou o arquivo. Upsert torna a repetição segura. Checkpoint de página só vale a pena se a coleta for longa; para as janelas abaixo, recomeçar é mais simples e evita página pulada se um registro novo entrar no meio da ordenação.
- Ordenar a lista por `id` ascendente quando o endpoint permitir, para a página já lida não mudar quando surge id novo no fim.
- Uma execução por domínio, com registro de início, fim, janela, status, páginas, itens e erro. Falha de despesa não impede deputado.
- Retentativa só para erro transitório (secção 2).
- Concorrência: uma coleta por vez, trava no banco se houver mais de uma instância, como o ADR 0002 já prevê. Sem fila nova.
- Espelho oficial e "atualização relevante" são tabelas diferentes. O espelho acompanha a fonte, inclusive correção retroativa. O log de mudança da aplicação só acrescenta o que o próprio sistema observou entre duas coletas bem-sucedidas.

### Por conjunto

| Conjunto | Carga inicial | Atualização | Por que |
|---|---|---|---|
| Legislatura 57, partidos citados, referências usadas | API, poucas páginas | Diária ou quando um id novo aparecer | Volume pequeno. Arquivo de legislaturas existe e não foi validado |
| Deputados em exercício | `GET /deputados` com `itens=100`, depois detalhe e histórico de cada id | Diária, de novo a lista inteira (6 páginas) e o detalhe/histórico de cada um | 513 pessoas. Sem delta. A lista não traz situação |
| Proposições | Não varrer desde 1934 pela API. Para o MVP, janela de tramitação mais arquivos anuais recentes se a carga inicial precisar de ementa fora da janela | Diária: `dataInicio`/`dataFim` com sobreposição de alguns dias sobre a última janela concluída, sempre dentro do que a primeira página informar em `X-Total-Count`. Para cada id novo ou já visto na janela: detalhe, autores e tramitações | Sem data de alteração. O arquivo anual não traz o histórico de passos. Proposição que saia da janela permanece no banco |
| Votações e votos | Arquivos anuais para anos fechados, depois de conferir o cabeçalho. Ano corrente pela API, mês a mês, um ano por vez | Diária: janela dos últimos dias **dentro do ano corrente**, detalhe, votos e orientações de cada id | A API recusa, no texto, intervalo entre anos. A lista sem detalhe perde a proposição. Voto simbólico pode ganhar linha depois |
| Despesas | Arquivo `Ano-{ano}.csv.zip` do ano corrente e, se o MVP mostrar mandato inteiro, de 2023 em diante (legislatura 57). Anos anteriores só se o produto for exibir histórico | Diária no ano corrente: baixar o ZIP de novo e substituir o espelho daquele ano. Anos fechados: semanal ou sob demanda | Um download contra milhares de chamadas. `ano` sem `idLegislatura` veio vazio. O arquivo não tem id único; usar o hash da secção 7 |
| Eventos e presença | Não importar no MVP | — | O dado não sustenta indicador sem regra extra |

Sobreposição recomendada da janela de proposições e votações: 7 dias sobre a última execução **bem-sucedida**. O número não é da Câmara; é folga local para registro atrasado. Se a execução falhar, a janela seguinte continua a partir da última que terminou com sucesso, não do dia em que o job quebrou.

Deputado que desapareça da lista em exercício: manter o registro e a data em que deixou de vir. Não apagar. Proposição fora da janela: manter. Linha de despesa que suma do arquivo daquele ano: sai do espelho daquele ano na substituição completa do arquivo, porque o arquivo é o conjunto oficial daquele ano, não um incremental. Registrar a execução que fez a substituição.

Custo de pedido, em ordem de grandeza, sem concorrência e sem contar retentativa:

- Deputados: cerca de 6 + 513 + 513 pedidos por dia.
- Votações: 78 em 26 dias de outubro/2026 na lista; o detalhe e os votos multiplicam pelo número de votações da janela. Medir `X-Total-Count` antes de paralelizar.
- Proposições: o total de uma janela de 30 dias não foi medido. Um único deputado tem 635 proposições no histórico inteiro; isso não é a janela.
- Despesas por API na legislatura 57: 1.245 lançamentos num deputado. Inadequado como caminho principal.
- Despesas por arquivo: um ZIP de cerca de 4 MB no ano de 2026.

Não foi vista cota oficial. Manter um cliente sequencial no começo. Paralelismo só depois de observar ausência de `429` com o volume real, e ainda baixo.

Dados de hoje e dados históricos não vêm do mesmo atalho. Lista de deputados sem data é o exercício atual. Histórico e `idLegislatura` trazem mandato anterior. Votação e proposição sem data são a janela recente, não o arquivo. Despesa recente e despesa de 2008 são o mesmo tipo de arquivo, um por ano; a API não se comportou como índice confiável por ano.

## 10. Cliente Java

Módulo `importacao`, adaptador HTTP fora do domínio. Um cliente por recurso, interface pequena, teste com fixture JSON sem rede.

Comportamento mínimo do primeiro cliente (`GET /deputados`):

1. `Accept: application/json`. Base `https://dadosabertos.camara.leg.br/api/v2`.
2. `itens=100`, `ordem=asc`, `ordenarPor=nome` ou `id`.
3. Ler `dados`, `links` e `X-Total-Count` da primeira página.
4. Seguir `rel=next` até desaparecer ou até `dados` vir vazio.
5. Upsert por `id`. Ignorar página repetida.
6. Não mapear `cpf` quando o detalhe entrar. Fixture de detalhe já sem esse campo.
7. `400`: falhar a execução, sem retentativa. `404`: pular o id. `5xx` e `429`: retentativa com espera crescente.
8. Gravar a URL pedida e o instante da coleta.

Não gerar entidade JPA nesta etapa. O teste de contrato compara a fixture com o parser da paginação: duas páginas encadeiam, a página vazia encerra, e o total zero da página extra não substitui o total da primeira.

### Implementado em 2026-10-26

`ClienteDeputadosCamara` em `backend/`. A paginação não mudou. A aplicação Spring Boot 3.5.7 em Java 21 só registra esse cliente no contexto; a subida não coleta e não abre banco. Jackson continua no cliente porque o JDK não lê JSON.

Comportamento do código, coberto por fixture local:

- A primeira URL pede `itens=100`, `ordem=asc` e `ordenarPor=id`.
- A página seguinte é o `href` de `rel=next` no JSON. O cliente não remonta a query e não lê o cabeçalho HTTP `Link`.
- `dados` vazio encerra a coleta, mesmo que essa página ainda traga `next`.
- Sem `rel=next`, a coleta também encerra.
- O total guardado é o primeiro `X-Total-Count` positivo de uma página com deputados. Página vazia, zero, total ausente ou texto inválido não substituem esse valor. Se nenhuma página com deputados informar total positivo, o resultado fica com total nulo.
- Id repetido permanece uma vez, com os campos da primeira ocorrência.
- Status diferente de 200, falha de transporte, JSON inválido ou corpo fora do contrato lançam `FalhaColetaDeputados`. Não há lista vazia no lugar do erro.
- Um `next` que repete URL já pedida também falha, para a coleta não entrar em ciclo.

Ainda não há retentativa, registro da data da coleta nem leitura do detalhe do deputado. Isso fica para a execução da importação.

A coleta completa contra a API real é `ClienteDeputadosCamaraIT`. O Surefire exclui `**/*IT.java` em `mvn test` e no `package`. Comando:

```bash
mvn -f backend/pom.xml test -Pintegracao
```

## 11. Limitações que o produto precisa respeitar

- A API muda sem política de versão além do aviso de que a 0.4.x é incompleta. O adaptador isola isso.
- Votação na lista pode não apontar proposição. Objeto possível não é objeto confirmado.
- Voto individual não existe para toda votação. Ausência não é um registro.
- Presença em evento não é voto e não traz flag própria.
- Despesa não tem id oficial único. `ideDocumento` repetido é dado real (restituição), não lixo a deduplicar.
- API de despesa e CSV do mesmo deputado e ano não bateram na contagem (186 contra 147). A origem exibida na interface tem de ser o arquivo ou a URL do documento, não uma mistura silenciosa.
- `codSituacao` de proposição é oficialmente pouco confiável como filtro.
- Tramitação é descrita como representação provisória.
- Histórico de deputado vem fora de ordem e, segundo o OpenAPI, de um atalho GraphQL.
- Licença de reuso, SLA e cota de requisição continuam desconhecidos.
- Não há como pedir só o que mudou desde a última coleta.

## 12. Registro das verificações

| Data | O que foi lido | Resultado |
|---|---|---|
| 2026-10-26 | OpenAPI `0.4.347` e HTML do Swagger, secção Arquivos | Endpoints, parâmetros, arquivos anuais e avisos de votação, cota e presença |
| 2026-10-26 | Chamadas reais listadas nas secções 4 a 8, mais página além do fim e filtros de despesa | Paginação fechada; ligações entre ids confirmadas; `ano` de despesa sem legislatura veio vazio |
| 2026-10-26 | `Ano-2026.csv.zip` | 118.559 linhas; chave única só no conjunto das colunas, sem usar `cpf` |

Amostras brutas ficaram fora do repositório porque incluem CPF e documento de fornecedor. Fixture de teste entra no código só depois de removidos esses campos.

## Referências

- https://dadosabertos.camara.leg.br/swagger/api.html
- https://dadosabertos.camara.leg.br/api/v2/api-docs
- https://dadosabertos.camara.leg.br/howtouse/2020-02-07-dados-votacoes.html (citado pelo OpenAPI; não relido integralmente nesta verificação)
- https://dadosabertos.camara.leg.br/howtouse/2023-12-26-dados-ceap.html
- https://www.camara.leg.br/cotas/Ano-2026.csv.zip
