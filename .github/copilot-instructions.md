# Instruções permanentes para o Copilot – Política Aberta

Projeto de código aberto de transparência política no Brasil. Leia `README.md` e `docs/` antes de propor mudanças.

## Idioma

- Documentação, mensagens de commit, issues, PRs e comentários de código em **português brasileiro**.
- Identificadores de código (classes, métodos, variáveis) em inglês ou português, mantendo consistência com o código existente. O domínio pode usar termos em português (ex.: `Deputado`, `Proposicao`) quando facilitar a relação com a fonte oficial.

## Arquitetura (não violar)

- **Monólito modular.** Não criar microsserviços, filas ou componentes de infraestrutura novos sem um ADR aprovado em `docs/decisions/`.
- Módulos por domínio (ex.: parlamentares, proposições, votações, despesas, acompanhamento, importação). Cada módulo expõe uma API pública mínima; não acessar tabelas ou classes internas de outro módulo.
- Camadas: `web` (controllers/DTOs) → `application` (casos de uso) → `domain` → `infrastructure` (JPA, clientes HTTP). Dependências apontam para dentro; o domínio não depende de Spring Web.
- Integrações com fontes oficiais ficam isoladas em adaptadores de importação, separados da API pública e do modelo de domínio.
- Toda alteração de esquema de banco é uma migração **Flyway** versionada; nunca editar migração já publicada.
- Toda mudança arquitetural relevante exige novo ADR (`docs/decisions/NNNN-titulo.md`) com contexto, alternativas, decisão e consequências.

## Dados oficiais

- **Nunca inventar** endpoints, parâmetros, campos ou formatos de resposta de APIs oficiais. Basear-se na documentação oficial ou em respostas reais verificadas, e registrar em `docs/data-sources/` o que foi confirmado e o que segue incerto.
- Guardar a origem (URL/identificador oficial) e a data de importação dos dados.
- Importações devem ser idempotentes, respeitar limites da API e registrar execuções e falhas.
- Neutralidade: apresentar dados, sem juízo de valor, ranking subjetivo ou viés partidário.

## Testes

- Todo código novo ou alterado vem com testes (JUnit no backend; testes de componentes/unidade no frontend).
- Testes de integração com banco usam PostgreSQL real (ex.: Testcontainers), não substitutos em memória, salvo decisão em ADR.
- Testes de clientes de API oficial usam respostas gravadas (fixtures) e nunca dependem da rede.
- Corrigir bug = adicionar teste que reproduz o bug.
- Não desabilitar nem remover testes para fazer o build passar.

## Dependências

- Não adicionar dependência sem justificativa (no PR ou ADR): problema resolvido, alternativas, licença compatível e manutenção ativa.
- Preferir recursos do JDK, Spring Boot e React antes de bibliotecas extras.
- Não commitar segredos; usar variáveis de ambiente e `.env.example`.

## Documentação

- Atualizar a documentação no mesmo PR que altera comportamento, arquitetura, fontes de dados ou como rodar o projeto (`README.md`, `docs/`).
- Manter o roadmap refletindo o estado real.
- Registrar incertezas técnicas em vez de assumir.

## Fluxo de trabalho

- Mudanças pequenas e focadas, uma preocupação por PR.
- Rodar build e testes antes de propor a mudança.
- Não implementar funcionalidades fora da fase atual do roadmap sem solicitação explícita.
- Não executar commit ou push sem pedido do usuário.
