# ADR 0003 – Persistência de deputados

- **Status:** Proposta
- **Data:** 2026-10-26

## Contexto

O cliente de `GET /deputados` já entrega a lista em exercício. O PostgreSQL e o Flyway existem, sem tabela de domínio. A importação agendada continua a do [ADR 0002](0002-estrategia-de-importacao.md) e ainda não foi implementada. Esta decisão cobre só o modelo e a gravação de um deputado.

## Alternativas

1. **JPA/Hibernate.** Mapeamento curto, mas traz um framework novo para uma tabela e um upsert.
2. **JDBC com SQL parametrizado (escolhida).** O `spring-boot-starter-jdbc` já está no projeto.
3. **Guardar o JSON da lista.** Preserva a resposta, mas dificulta consulta e duplica o que já cabe em colunas.

## Decisão

JDBC. A tabela `deputado` nasce na migração `V2__criar_deputado.sql`. A `V1` não muda.

A chave primária é `id_camara`, o `id` da lista oficial (o mesmo `ideCadastro` da cota). Não há chave interna separada: esse número é estável, único e é a chave do upsert futuro. Outros módulos devem referenciá-lo, não uma coluna paralela.

`nome` é o único nome da lista. Nome civil e nome parlamentar só aparecem no detalhe, que este cliente não lê. Não criar essas colunas agora.

`email` entra porque a lista já o traz e ele é o contato público do gabinete. `uri` entra como URL de origem. `uriPartido` fica de fora: a sigla do partido já identifica o vínculo desta etapa, e a URL do partido não é necessária para consultar o deputado.

Não há CPF, JSON bruto nem coluna de nome duplicado.

`atualizado_em` é o instante local da última gravação, informado por quem chama `salvar`. Na primeira inclusão e nas seguintes ele é reescrito. A data em que o deputado deixou de aparecer na lista continua para a importação, como o ADR 0002 já descreve.

`salvar` faz `INSERT ... ON CONFLICT (id_camara) DO UPDATE`. Repetir o mesmo id oficial substitui as colunas e não cria outra linha.

A listagem ordena por `id_camara` crescente.

## Consequências

- JPA não entra nesta etapa. Outro módulo pode propor JPA num ADR próprio.
- O repositório só existe quando `politica-aberta.banco` não está `false`. O padrão é ligado. A suíte `mvn test` desliga essa propriedade e não abre PostgreSQL.
- A prova de SQL real fica no perfil Maven `banco`, com Testcontainers 1.21.4 e PostgreSQL 17.11. O Spring Boot 3.5.7 gerencia a 1.21.3; esta fica um patch acima porque a 1.21.3 só sonda a API Docker 1.32, recusada por daemons que exigem 1.40 ou mais. Ela não chama a Câmara e não entra em `mvn test` nem em `package`.
