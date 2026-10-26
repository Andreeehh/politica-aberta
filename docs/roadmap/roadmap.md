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

O backend executável está em Java 21 e Spring Boot 3.5.7, sem banco e sem API REST. O restante desta fase não começou.

- [x] Esqueleto executável do backend (Java 21, Spring Boot 3.5.7, Maven), sem banco e sem endpoints.
- Frontend (React, TypeScript, Vite).
- Validar compatibilidade de Java 25 com Spring Boot e plugins; senão, adotar Java 21 LTS.
- Docker Compose com PostgreSQL para desenvolvimento.
- Flyway com migração inicial.
- GitHub Actions: build e testes do backend e do frontend.
- Endpoint de saúde e página inicial mínima.
- `CONTRIBUTING.md`, código de conduta e modelos de issue/PR.

## Fase 2 – Parlamentares

- [x] Cliente de leitura de `GET /deputados`, com paginação por `rel=next` e testes sem rede. Sem persistência.
- Importação de deputados federais (após investigar a API).
- Listagem e perfil com informações do mandato.
- Links para a fonte oficial e data da última atualização.

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
