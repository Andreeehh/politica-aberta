# Roadmap

Roadmap preliminar, sujeito a revisão. Cada fase só começa após a anterior ser validada. Estado atual: **Fase 0**.

## Fase 0 – Planejamento (atual)

- [x] Estrutura do repositório.
- [x] README, licença provisória, `.gitignore`.
- [x] Instruções permanentes para o Copilot.
- [x] Documentos de arquitetura, fonte de dados e ADRs iniciais.
- [ ] Confirmar licença (ver ADR 0001).
- [x] Investigar incertezas técnicas listadas em `docs/data-sources/camara.md` (verificação de 2026-10-26; licença de reuso, SLA e cota de requisições continuam desconhecidos).

## Fase 1 – Fundação técnica

O backend executável está em Java 21 e Spring Boot 3.5.7. A API própria expõe saúde, consulta de deputados persistidos e a importação manual. O PostgreSQL local sobe com Docker Compose. Não há página inicial nem agendamento.

- [x] Esqueleto executável do backend (Java 21, Spring Boot 3.5.7, Maven), sem banco e sem endpoints.
- Frontend (React, TypeScript, Vite).
- Validar compatibilidade de Java 25 com Spring Boot e plugins; senão, adotar Java 21 LTS.
- [x] Docker Compose com PostgreSQL 17.11 para desenvolvimento, volume nomeado e healthcheck.
- [x] Flyway com migração inicial inofensiva, sem tabelas de domínio.
- GitHub Actions: build e testes do backend e do frontend.
- [x] Endpoint de saúde `GET /api/health`, com `{"status":"UP"}`, sem banco e sem fonte externa.
- Página inicial mínima.
- `CONTRIBUTING.md`, código de conduta e modelos de issue/PR.

## Fase 2 – Parlamentares

- [x] Cliente de leitura de `GET /deputados`, com paginação por `rel=next` e testes sem rede.
- [x] Tabela `deputado` e repositório JDBC idempotente.
- [x] Importação manual de deputados federais, com histórico da execução. Sem agendamento.
- [x] `GET /api/deputados` e `GET /api/deputados/{id}`, com paginação, filtro de UF e partido, `uri` e `atualizadoEm`.
- Perfil com informações do mandato que só existem no detalhe da Câmara.

## Fase 3 – Proposições e votações

- Importação de proposições e tramitações.
- Votações nominais e votos individuais.

## Fase 4 – Despesas parlamentares

- Importação e consulta de despesas, com filtros básicos.
- Avaliar volume de dados e estratégia de carga (ver ADR 0002).

## Fase 5 – Acompanhamento e atualizações

- Seguir deputados e assuntos de interesse.
- Registro de atualizações relevantes.
- Decisão sobre autenticação/identidade (ADR próprio, incluindo LGPD).

## Fase 6 – Resumos semanais

- Geração de resumos semanais por interesse.
- Decisão sobre canal de entrega (e-mail ou outro) e custo (ADR próprio).

## Fase 7 – Lançamento aberto

- Publicação, hospedagem de baixo custo, observabilidade básica.
- Modelo de sustentabilidade financeira transparente.

## Fora do escopo do MVP

Outras casas legislativas (Senado, assembleias, câmaras municipais), candidatos e eleições, e conteúdo editorial ou recomendações de voto.
