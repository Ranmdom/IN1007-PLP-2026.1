# Fase 4 - Plano de Implementacao do Linter

## Objetivo

Implementar o `VisitorLinter`, que percorre a AST ja validada semanticamente pela Fase 3 e emite **avisos de qualidade** sem abortar a execucao. As tres regras a implementar sao:

1. **Variavel nao utilizada** — variavel ou parametro declarado que nunca e lido
2. **Codigo morto** — ramo ou corpo inalcancavel porque a condicao e uma constante booleana
3. **Complexidade de procedimento** — procedimento com muitos pontos de decisao acima de um limiar configuravel

Separacao obrigatoria de responsabilidades:

- `ERRO SEMANTICO [CODIGO]:` — saida do `VisitorSemantico` (Fase 3), aborta a execucao
- `LINT [CODIGO]:` — saida do `VisitorLinter` (Fase 4), apenas informa, execucao continua

## 1. Arquitetura

### 1.1. Pipeline atualizado

```text
Codigo-fonte
  -> Imp2Parser.Input()                [parser JavaCC, Fase 2]
  -> Programa(comando)                 [raiz da AST]
  -> VisitorSemantico.analisar()       [Fase 3 - ja existe]
       -> preenche TabelaSimbolos
       -> acumula List<ErroSemantico>
  -> se houver erros semanticos: imprime e aborta
  -> VisitorLinter.analisar()          [NOVO - Fase 4]
       -> le TabelaSimbolos preenchida pelo visitor semantico
       -> percorre AST por instanceof
       -> acumula List<AvisoLinter>
  -> imprime avisos (sem abortar)
  -> programa.checaTipo() + programa.executar() [legado]
```

### 1.2. Pacote

Pacote novo: `li2.plp.imperative2.linter`

### 1.3. Classes a criar

| Classe | Responsabilidade |
|---|---|
| `AvisoCodigo` | enum com os codigos dos avisos do linter |
| `AvisoLinter` | codigo + mensagem + contexto; analogo ao `ErroSemantico` da Fase 3 |
| `AvaliadorConstante` | helper que determina se uma expressao booleana e constante em tempo de compilacao |
| `VisitorLinter` | visitor principal: percorre AST e acumula a lista de avisos |

**Nota:** a adicao de numeros de linha exigiu modificar varios arquivos existentes (ver secao 1.4).

## 2. Codigos de Aviso

| Codigo | Quando ocorre |
|---|---|
| `VAR_NAO_UTILIZADA` | variavel declarada com `lido == 0` ao fechar o escopo |
| `PARAM_NAO_UTILIZADO` | parametro de procedimento com `lido == 0` ao fechar o escopo |
| `CODIGO_MORTO_RAMO_THEN` | ramo `then` inalcancavel porque a condicao e sempre `false` |
| `CODIGO_MORTO_RAMO_ELSE` | ramo `else` inalcancavel porque a condicao e sempre `true` |
| `CODIGO_MORTO_WHILE` | corpo do `while` inalcancavel porque a condicao e sempre `false` |
| `COMPLEXIDADE_PROCEDIMENTO` | procedimento ultrapassa o limiar de pontos de decisao |

Formato textual de saida:

```text
LINT [VAR_NAO_UTILIZADA]: variavel 'temp' declarada mas nunca lida
LINT [CODIGO_MORTO_RAMO_ELSE]: (em 'calc') ramo 'else' inalcancavel porque a condicao e sempre 'true'
LINT [COMPLEXIDADE_PROCEDIMENTO]: (em 'calc') complexidade 6 ultrapassa o limite de 4
```

## 3. Regras do Linter

### 3.1. Regra 1 — Variavel nao utilizada

Apos a analise semantica, o `VisitorLinter` percorre todos os escopos da `TabelaSimbolos` e verifica os contadores `lido` e `escrito` de cada simbolo. A Fase 3 ja os incrementa durante a travessia semantica; a Fase 4 apenas os le.

- `VARIAVEL` com `lido == 0`: emite `VAR_NAO_UTILIZADA`
- `PARAMETRO` com `lido == 0`: emite `PARAM_NAO_UTILIZADO`
- `PROCEDIMENTO`: ignorado por esta regra
- Variavel apenas escrita (atribuida mas nunca lida) tambem recebe aviso, pois o resultado e descartado

Nao ha necessidade de percorrer a AST nesta regra — a tabela ja contem toda a informacao necessaria.

### 3.2. Regra 2 — Codigo morto

O `AvaliadorConstante` tenta determinar o valor booleano de uma expressao sem executar o programa. Ele avalia apenas literais (`true`, `false`) e composicoes deles (`not true`, `true and false`, etc.). Quando a expressao contem uma variavel ou qualquer sub-expressao nao-constante, retorna indefinido — sem aviso, sem falso positivo.

Casos detectados:

- `if true then C1 else C2` — ramo `else` nunca executa → `CODIGO_MORTO_RAMO_ELSE`
- `if false then C1 else C2` — ramo `then` nunca executa → `CODIGO_MORTO_RAMO_THEN`
- `while false do C` — corpo nunca executa → `CODIGO_MORTO_WHILE`
- `if not false then C1 else C2` — avaliavel como `if true` → `CODIGO_MORTO_RAMO_ELSE`

Mesmo ao detectar codigo morto, o linter continua visitando os ramos para que a Regra 3 contabilize a complexidade corretamente.

### 3.3. Regra 3 — Complexidade de procedimento

Metrica usada: **complexidade ciclomatica simplificada** — contagem de pontos de decisao dentro do corpo do procedimento.

- Ao entrar em um `proc`, o contador e zerado
- Cada `if` e cada `while` dentro do corpo incrementa o contador
- Ao sair do procedimento, se o contador ultrapassar o limiar (padrao: **4**), emite `COMPLEXIDADE_PROCEDIMENTO`
- Desvios fora de procedimentos (no corpo principal do programa) nao sao contabilizados

## 4. Integracao no Pipeline

Insercao em `Imperative2.jj`, logo apos o bloco do `VisitorSemantico`. O linter recebe a tabela de simbolos ja preenchida e analisa o programa. Os avisos sao impressos, mas a execucao **nao e abortada** — essa e a diferenca fundamental em relacao aos erros semanticos.

Modificacao auxiliar necessaria: adicionar o getter `getTabelaSimbolos()` no `VisitorSemantico` para expor a tabela ao linter.

## 5. Suite de Testes

Pasta: `PLP_2026/Imperativa2/testes-fase4/`

| Arquivo | Programa | Aviso esperado |
|---|---|---|
| `01_var_nao_utilizada.imp` | variavel declarada, nunca lida | `VAR_NAO_UTILIZADA` |
| `02_param_nao_utilizado.imp` | parametro de proc nunca lido | `PARAM_NAO_UTILIZADO` |
| `03_var_so_escrita.imp` | variavel atribuida mas nunca lida | `VAR_NAO_UTILIZADA` |
| `04_if_sempre_true.imp` | `if true then ... else ...` | `CODIGO_MORTO_RAMO_ELSE` |
| `05_if_sempre_false.imp` | `if false then ... else ...` | `CODIGO_MORTO_RAMO_THEN` |
| `06_while_sempre_false.imp` | `while false do ...` | `CODIGO_MORTO_WHILE` |
| `07_complexidade_alta.imp` | proc com 5 ifs/whiles | `COMPLEXIDADE_PROCEDIMENTO` |
| `08_complexidade_no_limite.imp` | proc com exatamente 4 desvios | nenhum aviso |
| `09_sem_avisos.imp` | programa valido que usa tudo corretamente | nenhum aviso |
| `10_multiplos_avisos.imp` | combina var nao usada + if true + proc complexo | 3 avisos |
| `11_not_false_morto.imp` | `if not false then ... else ...` | `CODIGO_MORTO_RAMO_ELSE` |
| `12_condicao_variavel_nao_morto.imp` | `if x then ...` onde x e variavel | nenhum aviso |

## 6. Dependencias da Fase 3

| Artefato da Fase 3 | Como a Fase 4 consome |
|---|---|
| `Simbolo.getLido()` / `Simbolo.getEscrito()` | Regra 1: identifica variaveis e parametros nao lidos |
| `TabelaSimbolos.getTodosEscopos()` | Regra 1: itera sobre todos os simbolos de todos os escopos |
| `Escopo` com `LinkedHashMap` | Regra 1: avisos na ordem de declaracao do codigo-fonte |
| Semantico passa sem erros | Precondition: linter so roda se a analise semantica foi bem-sucedida |
| Ponto de insercao em `Imperative2.jj` | Linter entra no mesmo padrao ja estabelecido pela Fase 3 |

## 7. Etapas de Implementacao

### Etapa 1 — Infraestrutura de avisos

Criar o pacote `li2.plp.imperative2.linter` com `AvisoCodigo` e `AvisoLinter`. Nada funcional ainda — apenas a estrutura de dados que todas as regras vao usar e o formato de saida.

### Etapa 2 — Getter na tabela

Adicionar `getTabelaSimbolos()` no `VisitorSemantico`. Pre-requisito para que o linter receba a tabela preenchida. E a unica modificacao em um arquivo existente da Fase 3.

### Etapa 3 — Regra 1: variavel nao utilizada

Criar o `VisitorLinter` com o metodo `verificarVariaveisNaoUtilizadas()`. Ele percorre `tabela.getTodosEscopos()`, filtra por `SimboloKind` e emite aviso para contadores `lido == 0`. Nao percorre AST — so le a tabela. E a regra mais simples porque a Fase 3 ja fez o trabalho de contagem.

### Etapa 4 — Regra 2: codigo morto

Criar o `AvaliadorConstante`. Depois, adicionar no `VisitorLinter` a visita a `IfThenElse` e `While`: se o avaliador retornar um valor definitivo, emite o aviso do ramo inalcancavel. Se retornar indefinido (expressao tem variavel), nao emite nada.

### Etapa 5 — Regra 3: complexidade de procedimento

Adicionar no `VisitorLinter` o controle de contexto de procedimento (nome atual e contador). Ao entrar em `DeclaracaoProcedimento`, zera o contador. A cada `IfThenElse` ou `While` dentro de um procedimento, incrementa. Ao sair, compara com o limiar e emite aviso se necessario.

### Etapa 6 — Integracao no pipeline

Inserir o bloco do linter no `main()` de `Imperative2.jj`: instancia o `VisitorLinter`, chama `analisar(programa)`, imprime os avisos. Sem `return` — avisos nao abortam a execucao.

### Etapa 7 — Testes

Criar a pasta `testes-fase4/` com os 12 arquivos da suite. Rodar cada um e confirmar que o aviso esperado aparece no programa correto e que os programas validos nao produzem nenhum aviso.

As etapas 1 e 2 sao pre-requisito para tudo. As etapas 3, 4 e 5 sao independentes entre si e podem ser implementadas em qualquer ordem. A etapa 6 so faz sentido apos as tres regras estarem prontas.

## 8. Criterio de Saida da Fase 4

- [ ] Pacote `li2.plp.imperative2.linter` criado com as 4 classes.
- [ ] Regra 1 operacional: variaveis e parametros nao lidos emitem aviso correto. Validado pelos testes 01, 02, 03.
- [ ] Regra 2 operacional: `if true`, `if false`, `while false` e composicoes constantes emitem aviso correto. Teste 12 confirma ausencia de falso positivo. Validado pelos testes 04, 05, 06, 11, 12.
- [ ] Regra 3 operacional: complexidade acima do limiar emite aviso; no limiar, nao emite. Validado pelos testes 07 e 08.
- [ ] Multiplos avisos acumulados em uma unica execucao. Validado pelo teste 10.
- [ ] Avisos nao abortam a execucao — o programa ainda executa e produz saida correta.
- [ ] Formato textual `LINT [CODIGO]: mensagem` uniforme em todos os avisos.
- [ ] Getter `getTabelaSimbolos()` adicionado ao `VisitorSemantico`.
