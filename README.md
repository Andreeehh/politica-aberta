# Política Aberta

Plataforma gratuita, de código aberto e apartidária de transparência política no Brasil. O objetivo é permitir que cidadãos acompanhem representantes eleitos, votações, propostas legislativas, despesas públicas e os temas de seu interesse, sempre com link para as fontes oficiais.

> **Status:** o backend sobe com Java 21 e Spring Boot 3.5.7, sem banco e sem API própria. O cliente de deputados continua no mesmo processo. Frontend, PostgreSQL e importação persistida ainda não existem.

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

O processo que sobe hoje usa **Java 21** e **Spring Boot 3.5.7**. A checagem de Java 25 continua pendente. PostgreSQL, Flyway, API própria e frontend ainda não entram neste executável.

## Documentação

- [Roadmap](docs/roadmap/roadmap.md)
- [Arquitetura](docs/architecture/architecture.md)
- [Fonte de dados: Câmara dos Deputados](docs/data-sources/camara.md)
- [ADR 0001 – Arquitetura inicial](docs/decisions/0001-arquitetura-inicial.md)
- [ADR 0002 – Estratégia de importação](docs/decisions/0002-estrategia-de-importacao.md)

## Estrutura do repositório

```
.github/copilot-instructions.md   Instruções permanentes para o Copilot
docs/                             Roadmap, arquitetura, fontes de dados e decisões (ADRs)
backend/                          Aplicação Spring Boot (Java 21), com o cliente de deputados
frontend/                         (futuro) aplicação React + TypeScript + Vite
```

## Backend

A aplicação não abre porta HTTP e não exige PostgreSQL nem rede para subir. O cliente de deputados entra no contexto, mas a subida não dispara coleta.

```bash
mvn -f backend/pom.xml test
mvn -f backend/pom.xml package
mvn -f backend/pom.xml spring-boot:run
```

`mvn test` e `package` não chamam a Câmara. A coleta real continua só no perfil `integracao`:

```bash
mvn -f backend/pom.xml test -Pintegracao
```

## Licença

O projeto adota provisoriamente a **GNU AGPL-3.0** (ver [LICENSE](LICENSE)). A decisão final está pendente de confirmação; veja a seção "Licença" no [ADR 0001](docs/decisions/0001-arquitetura-inicial.md).

## Como contribuir

O código existente é o cliente de leitura em `backend/`. Discussões sobre o roadmap, a arquitetura e as fontes de dados continuam bem-vindas via issues. Ao contribuir, siga as regras de [.github/copilot-instructions.md](.github/copilot-instructions.md), que valem também para contribuidores humanos: testes, documentação atualizada e respeito à arquitetura.

## Aviso

Este projeto não é afiliado à Câmara dos Deputados nem a qualquer órgão público. Os dados são de fontes oficiais, mas podem conter atrasos ou inconsistências; consulte sempre a fonte original.
