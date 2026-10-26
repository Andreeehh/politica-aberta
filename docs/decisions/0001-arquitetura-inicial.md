# ADR 0001 – Arquitetura inicial

- **Status:** Proposta (aguardando validação na Fase 1)
- **Data:** 2026-10-26

## Contexto

Projeto de código aberto, financiado de forma modesta e mantido por poucos contribuidores no início. Precisa de baixo custo operacional, onboarding simples e evolução gradual. Os dados vêm de fontes oficiais e são majoritariamente de leitura, com atualização periódica.

## Decisões

### 1. Monólito modular

**Alternativas:** (a) microsserviços; (b) monólito sem fronteiras; (c) monólito modular.

**Decisão:** (c). Um único deploy, com módulos por domínio e fronteiras explícitas.

**Justificativa:** microsserviços aumentam custo, complexidade operacional e de contribuição, sem necessidade de escala atual. Monólito sem fronteiras degrada com o tempo. O modular permite extrair serviços depois, se houver motivo mensurável.

**Consequências:** deploy e depuração simples; exige disciplina para respeitar fronteiras (verificação automatizada a avaliar); escala é vertical/horizontal do monólito inteiro.

### 2. Linguagem e plataforma do backend: Java + Spring Boot + Maven

**Alternativas:** Kotlin, Node.js/NestJS, Python/Django.

**Decisão:** Java com Spring Boot e Maven, conforme a preferência do mantenedor e o ecossistema maduro (JPA, Flyway, Bean Validation, JUnit).

**Versão do Java:** tentar **Java 25**; **só adotar se** Spring Boot, plugins Maven, imagem Docker e CI suportarem oficialmente. Caso contrário, **Java 21 LTS**. A verificação é tarefa da Fase 1; o ambiente local atual tem JDK 21. Até lá, nenhuma versão está definitivamente fixada.

**Consequências:** possível necessidade de downgrade para 21; perfil de memória maior que alternativas leves.

### 3. Banco de dados: PostgreSQL + Flyway

**Alternativas:** MySQL/MariaDB, SQLite, banco NoSQL.

**Decisão:** PostgreSQL com migrações Flyway.

**Justificativa:** dados relacionais (deputados, votos, despesas), consultas analíticas, busca textual nativa, ampla oferta gratuita de hospedagem. Flyway garante esquema versionado e auditável.

**Consequências:** requer Postgres em dev (Docker Compose) e nos testes de integração.

### 4. Frontend: React + TypeScript + Vite

**Alternativas:** Next.js (SSR), templates server-side (Thymeleaf), Vue/Svelte.

**Decisão:** SPA React + TypeScript + Vite.

**Justificativa:** ecossistema e base de contribuidores amplos; Vite simples. Hospedagem estática barata.

**Consequências:** SEO e compartilhamento de páginas de deputados são limitados em SPA; pode exigir pré-renderização ou SSR no futuro (a avaliar, novo ADR).

### 5. Infraestrutura de desenvolvimento e CI

Docker Compose para dependências locais (PostgreSQL) e GitHub Actions para CI. Sem Kubernetes ou orquestração por ora.

### 6. Licença

**Proposta provisória: GNU AGPL-3.0**, hoje no arquivo `LICENSE`.

**Alternativas:** MIT/Apache-2.0 (permissivas), GPL-3.0, AGPL-3.0.

**Justificativa da proposta:** a AGPL exige que versões modificadas oferecidas via rede também publiquem o código, o que protege a natureza aberta e transparente do projeto contra forks fechados.

**Pendente de confirmação pelo mantenedor.** Considerações: AGPL pode desencorajar algumas empresas e órgãos de adotarem o código; permissivas favorecem adoção, mas permitem apropriação fechada. Dados e conteúdo (documentação) podem ter licença separada (ex.: CC BY 4.0), também a decidir. Se mudar, substituir o `LICENSE`, atualizar o README e registrar neste ADR.

## Consequências gerais

- Baixa complexidade operacional e fácil de rodar localmente.
- Decisões adiadas conscientemente: autenticação, e-mail de resumos, hospedagem, SSR.
- Qualquer divergência relevante desta arquitetura exige novo ADR.
