# ADR 0002 – Estratégia de importação de dados

- **Status:** Proposta (fonte verificada em 2026-10-26; a decisão abaixo permanece, ver secção final)
- **Data:** 2026-10-26

## Contexto

Os dados oficiais da Câmara são a base do produto. A aplicação precisa exibi-los rapidamente, relacioná-los (deputado ↔ votos ↔ proposições ↔ despesas), calcular atualizações e resumos e não depender da disponibilidade da fonte em cada requisição. Os endpoints confirmados estão em [docs/data-sources/camara.md](../data-sources/camara.md). Esta decisão define princípios, não o cliente HTTP.

## Alternativas consideradas

1. **Proxy em tempo real:** cada requisição do usuário consulta a API oficial.
   - Prós: sem armazenamento; sempre atual.
   - Contras: latência, dependência de disponibilidade e limites da fonte, impossibilidade de consultas relacionais e de detectar mudanças.
2. **Importação agendada para banco próprio (escolhida).**
   - Prós: desempenho, consultas livres, histórico, detecção de mudanças, resiliência.
   - Contras: dados podem estar defasados; necessidade de armazenamento e reconciliação.
3. **Pipeline separado (ETL/serviço dedicado, filas).**
   - Prós: isolamento.
   - Contras: complexidade e custo desnecessários no MVP.
4. **Apenas carga manual de arquivos em massa.**
   - Prós: simples.
   - Contras: dados desatualizados, trabalho manual.

## Decisão

Importação **agendada dentro do monólito** (módulo `importacao`), gravando em PostgreSQL. Princípios:

- **Idempotência:** reexecutar uma importação não duplica nem corrompe dados (upsert por identificador oficial).
- **Carga inicial + incremental:** carga histórica por arquivo anual, onde a Câmara publica um, e atualização por janela reimportada. A fonte não oferece filtro de alterados desde a última coleta. O detalhe por domínio está na verificação de 2026-10-26, no fim deste ADR.
- **Adaptadores isolados:** cliente HTTP e mapeamento da fonte ficam separados do domínio; mudanças na fonte afetam só o adaptador.
- **Rastreabilidade:** tabela de execuções (início, fim, fonte, status, contagens, erros) e guarda de identificador oficial, URL de origem e data de importação por registro.
- **Resiliência:** retentativas com espera crescente, respeito a limites da fonte, falha de um domínio não bloqueia os demais.
- **Reconciliação:** correção retroativa entra na releitura da mesma janela ou do mesmo arquivo. Remoção segue a política da secção final: substituir coleção filha; não apagar deputado nem proposição só porque saíram da consulta corrente.
- **Agendamento:** mecanismo simples do Spring (`@Scheduled`) com proteção contra execução concorrente (ex.: trava no banco) caso haja mais de uma instância. Avaliar Quartz/ShedLock apenas se necessário, com justificativa.
- **Detecção de mudanças:** gerar eventos de "atualização relevante" comparando estado anterior e novo, para alimentar acompanhamento e resumos semanais.
- **Testes:** com fixtures de respostas oficiais verificadas, sem acesso à rede.

## Consequências

- Dados exibidos podem ter defasagem; a interface mostra "atualizado em" e link para a fonte oficial.
- Aumenta o volume em banco (principalmente despesas); precisa de planejamento de índices e retenção.
- Mudanças na API oficial exigem manutenção do adaptador; testes de contrato com fixtures ajudam a detectar.
- Escalabilidade do importador está limitada à instância do monólito; extrair em serviço separado só com novo ADR.

## Verificação da fonte em 2026-10-26

A investigação registrada em [docs/data-sources/camara.md](../data-sources/camara.md) não muda a decisão deste ADR (importação agendada no monólito, upsert, adaptador isolado, execuções rastreáveis). Ela responde o que estava em aberto sobre a fonte:

- Não há filtro de registros alterados desde a última coleta. A atualização incremental é releitura de uma janela de tramitação ou de data, ou substituição do arquivo anual.
- Arquivos consolidados existem e, para a cota parlamentar, são o caminho adequado. A API de despesas, em 2026-10-26, não devolveu dados quando `ano` ou `mes` foram usados sem `idLegislatura`, ao contrário do texto oficial.
- Despesa não tem identificador oficial único. A idempotência desse domínio usa hash do conteúdo do arquivo, sem a coluna de CPF. Isso é consequência da fonte, não uma segunda estratégia de importação.
- Votação listada pode omitir a proposição. O vínculo fica no detalhe, separado entre objeto possível e proposição afetada.
- Cota numérica de requisições, licença de reuso e SLA não estão documentados no OpenAPI obtido.

Recomendações operacionais, ainda sem virar outra decisão: carga diária e completa dos deputados em exercício; janela diária com sobreposição para proposições e votações; arquivo anual diário para a despesa do ano corrente. Frequência fina do agendador continua escolha de implementação dentro dessas recomendações.

### Remoções e correções retroativas

Política compatível com a fonte, para valer na implementação:

- Coleção filha lida por inteiro (votos de uma votação, tramitações de uma proposição, histórico de um deputado, linhas de um arquivo anual de despesa) é substituída pelo último conjunto obtido com sucesso.
- Deputado que saia da lista de quem está em exercício permanece no banco, com a data da coleta em que deixou de aparecer.
- Proposição que saia da janela de tramitação permanece. Ausência na janela não é exclusão.
- Correção retroativa da Câmara entra na próxima leitura da mesma janela ou do mesmo arquivo. O espelho acompanha a fonte. O registro de atualização relevante da aplicação é outro fato, só de inclusão, e compara o estado anterior com o novo.

### O que continua em aberto

- Expressão cron exata e fuso do agendador.
- Ferramenta de métrica e alerta. O mínimo já decidido é a tabela de execuções; painel ou serviço externo exigiria outro ADR.
- Licença de reuso dos dados da Câmara, que não veio no material verificado.
