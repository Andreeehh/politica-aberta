# Política Aberta

Plataforma gratuita, de código aberto e apartidária de transparência política no Brasil. O objetivo é permitir que cidadãos acompanhem representantes eleitos, votações, propostas legislativas, despesas públicas e os temas de seu interesse, sempre com link para as fontes oficiais.

> **Status:** o backend sobe com Java 21 e Spring Boot 3.5.7, consulta deputados no PostgreSQL e importa a lista da Câmara só quando o endpoint administrativo é chamado. Não há frontend, autenticação nem agendamento.

## Princípios

- **Independência:** sem vínculo com partidos, candidatos ou governos. Nenhum conteúdo editorial partidário.
- **Transparência:** código, decisões técnicas e critérios de tratamento de dados são públicos.
- **Fonte oficial:** todo dado exibido deve apontar para sua origem e indicar quando foi atualizado.
- **Neutralidade:** apresentamos fatos e dados; não pontuamos nem rotulamos políticos de forma subjetiva.
- **Sustentabilidade:** custo operacional baixo e modelo de financiamento aberto e auditável (a definir).
- **Comunidade:** contribuições são bem-vindas.

## Escopo do MVP

Acompanhar **deputados federais** com dados oficiais da Câmara dos Deputados:

- Listar deputados e exibir perfil e mandato.
- Consultar proposições e tramitações.
- Consultar votações nominais e votos individuais.
- Consultar despesas parlamentares.
- Acompanhar deputados e assuntos de interesse.
- Registrar atualizações relevantes e preparar resumos semanais.
- Disponibilizar links para as fontes oficiais.

Detalhes em [docs/roadmap/roadmap.md](docs/roadmap/roadmap.md).

## Tecnologias previstas

| Camada | Tecnologia |
|---|---|
| Backend | Java (25 se compatível, senão 21 LTS), Spring Boot, Maven, Spring Web, Spring Data JPA, Bean Validation, JUnit |
| Banco de dados | PostgreSQL, Flyway |
| Frontend | React, TypeScript, Vite |
| Infraestrutura | Docker Compose (desenvolvimento), GitHub Actions (CI) |
| Arquitetura | Monólito modular com importação agendada |

O processo que sobe hoje usa **Java 21** e **Spring Boot 3.5.7**. A API própria expõe saúde, consulta de deputados e a importação manual. O PostgreSQL 17.11 local e o Flyway 11.7.2 já entram neste executável. A checagem de Java 25 e o frontend continuam pendentes.

## Documentação

- [Roadmap](docs/roadmap/roadmap.md)
- [Arquitetura](docs/architecture/architecture.md)
- [Fonte de dados: Câmara dos Deputados](docs/data-sources/camara.md)
- [ADR 0001 – Arquitetura inicial](docs/decisions/0001-arquitetura-inicial.md)
- [ADR 0002 – Estratégia de importação](docs/decisions/0002-estrategia-de-importacao.md)
- [ADR 0003 – Persistência de deputados](docs/decisions/0003-persistencia-de-deputados.md)

## Estrutura do repositório

```
.github/copilot-instructions.md   Instruções permanentes para o Copilot
docs/                             Roadmap, arquitetura, fontes de dados e decisões (ADRs)
backend/                          Aplicação Spring Boot (Java 21), com o cliente de deputados
docker-compose.yml                PostgreSQL 17.11 para desenvolvimento
.env.example                      Variáveis locais de exemplo
frontend/                         (futuro) aplicação React + TypeScript + Vite
```

## Pré-requisitos

- JDK 21
- Maven
- Docker com Compose, só para o PostgreSQL local

## Ambiente local

```bash
cp .env.example .env
```

O `.env` fica fora do Git. Os valores de `.env.example` são só de desenvolvimento: banco `politica_aberta`, usuário `politica_aberta` e senha `politica_aberta_dev`, na porta `5432`.

| Variável | Uso |
|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT` | Container PostgreSQL |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Conexão do backend |

## PostgreSQL

```bash
docker compose up -d
docker compose ps
docker inspect --format '{{.State.Health.Status}}' politica-aberta-postgres
```

O status saudável é `healthy`. Os dados ficam no volume nomeado `politica_aberta_postgres`. Reiniciar ou parar o container não apaga esse volume.

```bash
docker compose stop
```

`docker compose down` também preserva o volume nomeado. `docker compose down -v` remove os dados e não faz parte do uso normal.

## Backend

A aplicação escuta em `http://localhost:8080` quando o PostgreSQL está acessível. Sem as variáveis ou sem o banco, a subida falha. A subida não chama a Câmara. `GET /api/health` continua respondendo só se o processo HTTP está no ar.

```bash
set -a
source .env
set +a
mvn -f backend/pom.xml spring-boot:run
```

Saúde do processo:

```bash
curl http://localhost:8080/api/health
```

```json
{"status":"UP"}
```

As migrações ficam em `backend/src/main/resources/db/migration/`. `V2` cria `deputado`. `V3` cria `execucao_importacao`. Depois da subida com o banco no ar:

```bash
docker compose exec postgres psql -U politica_aberta -d politica_aberta -c "SELECT version, description, success FROM flyway_schema_history;"
```

## Deputados

A consulta lê só o PostgreSQL. `id` é o identificador oficial da Câmara. `pagina` começa em 1, `tamanho` padrão é 20 e o máximo é 100. A ordem é `id` crescente. `siglaUf` e `siglaPartido` são opcionais e ignoram maiúsculas.

```bash
curl "http://localhost:8080/api/deputados?pagina=1&tamanho=20&siglaUf=SP&siglaPartido=PT"
curl "http://localhost:8080/api/deputados/204379"
```

```json
{"pagina":1,"tamanho":20,"total":0,"dados":[]}
```

Um id ausente responde HTTP 404. Página, tamanho ou sigla inválidos respondem HTTP 400.

## Importação manual

`POST /api/admin/importacoes/deputados` percorre todas as páginas da lista oficial e grava cada deputado antes de pedir a próxima. A chamada fica aberta até o fim. Não há agendamento.

Este endpoint **não tem autenticação**. Não o exponha em produção antes do controle de acesso.

```bash
curl -X POST http://localhost:8080/api/admin/importacoes/deputados
```

```json
{"id":1,"tipo":"deputados","status":"concluida","iniciadaEm":"2026-10-26T21:00:00Z","terminadaEm":"2026-10-26T21:00:04Z","quantidade":513,"mensagemErro":null}
```

Se outra coleta de deputados já está em andamento, a resposta é HTTP 409. Se uma página falha, a resposta é HTTP 502, o status fica `falhou`, com `terminadaEm` e `mensagemErro`. Os deputados já gravados permanecem. Quem deixou de aparecer na lista também permanece. O histórico não guarda corpo HTTP nem credencial.

Uma execução que morre no meio pode continuar como `em_execucao` e bloquear a próxima. Nesse caso, o registro precisa ser encerrado no banco antes de um novo disparo.

```bash
docker compose exec postgres psql -U politica_aberta -d politica_aberta -c "SELECT id, tipo, status, iniciada_em, terminada_em, quantidade, mensagem_erro FROM execucao_importacao ORDER BY id DESC;"
```

O cliente não espera `Retry-After` em resposta HTTP 200. A fonte envia esse cabeçalho também no sucesso, e a documentação da Câmara não o trata como pausa obrigatória. Retentativa de HTTP 429 e 5xx ainda não está no cliente.

## Testes

`mvn test` e `package` não exigem PostgreSQL, Docker nem rede. Também não chamam a Câmara.

```bash
mvn -f backend/pom.xml test
mvn -f backend/pom.xml package
```

A coleta real da Câmara continua só no perfil `integracao`:

```bash
mvn -f backend/pom.xml test -Pintegracao
```

A integração do repositório com PostgreSQL usa Testcontainers e fica fora do comando acima. Ela exige Docker e não chama a Câmara:

```bash
mvn -f backend/pom.xml test -Pbanco
```

Campos, chave e upsert estão no [ADR 0003](docs/decisions/0003-persistencia-de-deputados.md).

## Licença

O projeto adota provisoriamente a **GNU AGPL-3.0** (ver [LICENSE](LICENSE)). A decisão final está pendente de confirmação; veja a seção "Licença" no [ADR 0001](docs/decisions/0001-arquitetura-inicial.md).

## Como contribuir

O código existente é o cliente de leitura em `backend/`. Discussões sobre o roadmap, a arquitetura e as fontes de dados continuam bem-vindas via issues. Ao contribuir, siga as regras de [.github/copilot-instructions.md](.github/copilot-instructions.md), que valem também para contribuidores humanos: testes, documentação atualizada e respeito à arquitetura.

## Aviso

Este projeto não é afiliado à Câmara dos Deputados nem a qualquer órgão público. Os dados são de fontes oficiais, mas podem conter atrasos ou inconsistências; consulte sempre a fonte original.
