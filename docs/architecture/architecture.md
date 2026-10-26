# Arquitetura

Visão inicial; será refinada na Fase 1. Decisões e justificativas em [docs/decisions](../decisions/).

## Visão geral

Monólito modular: uma aplicação Spring Boot, um banco PostgreSQL e um frontend React estático. A importação roda como tarefas agendadas dentro do próprio monólito.

```mermaid
flowchart LR
    Usuario[Cidadão] --> Front[Frontend React/TS]
    Front -->|HTTP/JSON| API
    subgraph Backend [Backend Spring Boot - monólito modular]
        API[Camada web] --> App[Casos de uso]
        App --> Dom[Domínio]
        App --> Infra[Persistência]
        Imp[Importação agendada] --> App
    end
    Infra --> DB[(PostgreSQL)]
    Imp -->|HTTP| Camara[Fontes oficiais da Câmara]
```

## Módulos previstos (backend)

| Módulo | Responsabilidade |
|---|---|
| `parlamentares` | Deputados e mandatos |
| `proposicoes` | Proposições e tramitações |
| `votacoes` | Votações nominais e votos individuais |
| `despesas` | Despesas parlamentares |
| `acompanhamento` | Seguir deputados e assuntos |
| `atualizacoes` | Registro de mudanças relevantes e resumos semanais |
| `importacao` | Clientes das fontes oficiais, agendamento, controle de execuções |
| `compartilhado` | Utilitários transversais mínimos |

Os módulos são pacotes Java dentro de um único artefato. Regras:

- Comunicação entre módulos só pela API pública do módulo (interfaces/serviços expostos).
- Sem acesso direto às tabelas de outro módulo.
- Avaliar uma verificação automática de fronteiras (ex.: ArchUnit ou Spring Modulith) na Fase 1, com justificativa de dependência.

## Camadas dentro de cada módulo

`web` → `application` → `domain` ← `infrastructure`. O domínio não depende de framework web.

## Estrutura prevista do repositório

```
backend/             Maven, Spring Boot
frontend/            Vite, React, TypeScript
docs/
docker-compose.yml   PostgreSQL 17.11 para desenvolvimento local
.env.example         Variáveis locais de exemplo; .env fica fora do Git
```

## Dados

- PostgreSQL como fonte única de verdade da aplicação; migrações com Flyway.
- Cada registro importado guarda identificador oficial, URL de origem e data de importação.
- A tabela `deputado` usa JDBC, não JPA. A chave primária é o id oficial da Câmara. Detalhe em [ADR 0003](../decisions/0003-persistencia-de-deputados.md).
- O frontend consome apenas a API própria, nunca as APIs oficiais diretamente.

## Importação

Ver [ADR 0002](../decisions/0002-estrategia-de-importacao.md).

A importação de deputados é explícita, síncrona e sem agendamento. Cada execução grava uma linha em `execucao_importacao` antes das requisições HTTP. O deputado é persistido página a página. Uma falha no meio não apaga o que já entrou e não marca a execução como concluída. O índice único de execução `em_execucao` impede duas coletas do mesmo tipo ao mesmo tempo. `POST /api/admin/importacoes/deputados` não tem autenticação.

## Qualidade e CI

- Testes JUnit. O `mvn test` padrão não abre PostgreSQL nem Docker. O repositório de deputados tem integração com PostgreSQL 17.11 via Testcontainers, só no perfil Maven `banco`.
- GitHub Actions: build e testes de backend e frontend em PRs.

## Segurança e privacidade

- Dados oficiais são públicos, mas dados de usuários (acompanhamentos, e-mail) serão pessoais: LGPD, coleta mínima, decisão própria em ADR antes da Fase 5.
- Sem segredos no repositório.

## Questões em aberto

- Mecanismo de verificação de fronteiras entre módulos.
- Estratégia de cache e paginação da API própria.
- Hospedagem e custo.
- Autenticação de usuários.
