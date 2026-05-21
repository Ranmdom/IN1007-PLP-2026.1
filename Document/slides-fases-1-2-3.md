# Slides de Apresentação — Fases 1, 2 e 3

## Analisador Estático e Linter para a Linguagem Imperativa 2

Roteiro de apresentação cobrindo o trabalho realizado nas três primeiras fases do projeto. Cada slide traz título e um texto descritivo, pensado para ser explicado em voz alta — não apenas lido.

---

## Slide 1 — Capa

**Título:** Analisador Estático e Linter para a Linguagem Imperativa 2

**Texto:**

Este projeto parte de uma base que já interpreta programas da Linguagem Imperativa 2 e a transforma em uma ferramenta de análise estática. Em vez de apenas executar o programa, a ferramenta passa a inspecionar a árvore sintática, validar regras semânticas e, futuramente, apontar problemas de qualidade de código.

A apresentação cobre as três fases já concluídas: o levantamento da base, a consolidação da gramática e da AST, e a estruturação da análise semântica.

---

## Slide 2 — Visão Geral da Arquitetura

**Título:** O Pipeline da Ferramenta

**Texto:**

A ferramenta é organizada como um pipeline em que cada etapa entrega um resultado para a próxima:

```text
Código-fonte
  → Análise léxica e sintática (JavaCC)
  → AST
  → Análise semântica
  → Análise de linter
  → Relatório de erros e avisos
```

A grande decisão de projeto foi separar responsabilidades. O **parser** apenas reconhece a linguagem e constrói a AST. A **análise semântica** valida declarações, escopo, tipos e chamadas de procedimento. O **linter** olha para a qualidade do código — variáveis não usadas, código morto, complexidade. Manter esses três passos isolados evita misturar "o programa está errado" com "o programa poderia ser melhor", que são diagnósticos de natureza diferente.

---

## Slide 3 — Fase 1: Objetivo

**Título:** Fase 1 — Levantamento e Entendimento da Base

**Texto:**

Antes de escrever qualquer código novo, foi preciso entender exatamente como o projeto existente estava montado. O objetivo da Fase 1 foi responder a quatro perguntas: onde a AST é construída, como a execução atual a consome, onde encaixar a análise semântica e onde encaixar o linter.

O resultado dessa fase não é código, e sim conhecimento: um mapa dos arquivos, um diagrama do pipeline atual e — o mais importante — uma lista clara das lacunas técnicas que precisariam ser resolvidas antes de avançar.

---

## Slide 4 — Fase 1: O Que Foi Mapeado

**Título:** Fase 1 — Mapa da Base de Código

**Texto:**

O levantamento mostrou que a AST já era razoavelmente completa. O ponto de entrada é o parser `Imperative2.jj`, que constrói objetos a partir da classe raiz `Programa`. A árvore tem nós para todos os comandos da linguagem (`IfThenElse`, `While`, `Atribuicao`, `Write`, `Read`, `SequenciaComando`, `ComandoDeclaracao`, `ChamadaProcedimento`), para as declarações (variável, composta, procedimento e parâmetros) e para as expressões (binárias, unárias, identificadores e valores).

Também foi mapeado o controle de escopo já existente: a classe `Contexto<T>`, uma pilha de `HashMap<Id, T>`. Ela é sólida, mas serve à execução e à checagem de tipo embutida — não guarda a informação extra que um analisador estático precisa.

---

## Slide 5 — Fase 1: Lacunas Identificadas

**Título:** Fase 1 — As Lacunas Técnicas

**Texto:**

O levantamento apontou três obstáculos concretos.

Primeiro, **a AST era opaca**: os nós guardavam seus filhos em campos `private` sem getters, então nenhum visitor conseguiria percorrê-la. Segundo, **o parser não tratava precedência**: todos os operadores binários estavam no mesmo nível, e os operadores unários eram "gulosos". Terceiro, **não existia infraestrutura de análise**: nem visitor, nem tabela de símbolos própria, nem classe de diagnóstico.

A Fase 1 também tomou uma decisão de arquitetura: o visitor seria implementado por `instanceof`, e não pelo padrão Visitor clássico com `accept()`. Isso evita modificar todas as classes da AST e é perfeitamente adequado ao número limitado de tipos de nó da linguagem.

---

## Slide 6 — Fase 2: O Problema da Gramática

**Título:** Fase 2 — Precedência e Associatividade Quebradas

**Texto:**

A Fase 2 começou corrigindo o parser. Na versão antiga, todos os operadores binários ficavam no mesmo nível de precedência, e cada operação era montada com recursão à direita. Isso produzia árvores erradas:

- `1 + 2 == 3` era lido como `1 + (2 == 3)` — e falhava, porque tentava somar um inteiro com um booleano.
- `true or false and true` não respeitava que `and` tem precedência maior que `or`.
- `1 - 2 - 3` virava `1 - (2 - 3)`, dando associatividade à direita onde deveria ser à esquerda.
- Os operadores unários `-`, `not` e `length` consumiam a expressão inteira à frente, então `-x + 1` virava `-(x + 1)`.

Em outras palavras, o parser aceitava os programas, mas construía a árvore com o significado errado.

---

## Slide 7 — Fase 2: Gramática Fatorada em 3 Níveis

**Título:** Fase 2 — A Solução na Gramática

**Texto:**

A correção foi fatorar a gramática de expressões em três níveis de precedência, do operador mais fraco para o mais forte:

```text
ExpBinaria  ::= ExpBinaria2 [ "==" ExpBinaria2 ]                    -- nível 1
ExpBinaria2 ::= ExpBinaria3 { ("+" | "-" | "or" | "++") ExpBinaria3 } -- nível 2
ExpBinaria3 ::= ExpUnaria  { "and" ExpUnaria }                       -- nível 3
```

Como cada nível só monta o nó depois de resolver o nível mais forte, a precedência fica garantida: o `==` é sempre o último a ser aplicado, e o `and` antes do `or`. A associatividade à esquerda nos níveis 2 e 3 é obtida com um laço e um acumulador, em vez de recursão à direita.

Os operadores unários foram corrigidos para chamar apenas a expressão **primária** à frente, de modo que `-x + 1` passa a ser `(-x) + 1`. A recursão à esquerda continua eliminada, porque nenhuma produção começa chamando a si mesma.

---

## Slide 8 — Fase 2: Abertura da AST

**Título:** Fase 2 — AST Acessível aos Visitors

**Texto:**

Para que um visitor consiga descer pela árvore, cada nó precisa expor os seus filhos. A Fase 2 adicionou getters públicos às 11 classes que estavam fechadas. A tabela abaixo é o contrato exato do que foi aberto:

| Classe | Getters adicionados |
|---|---|
| `Programa` | `getComando()` |
| `IfThenElse` | `getExpressao()`, `getComandoThen()`, `getComandoElse()` |
| `While` | `getExpressao()`, `getComando()` |
| `Atribuicao` | `getId()`, `getExpressao()` |
| `Write` | `getExpressao()` |
| `Read` | `getId()` |
| `SequenciaComando` | `getComando1()`, `getComando2()` |
| `ComandoDeclaracao` | `getDeclaracao()`, `getComando()` |
| `ChamadaProcedimento` | `getNomeProcedimento()`, `getParametrosReais()` |
| `DeclaracaoComposta` | `getDeclaracao1()`, `getDeclaracao2()` |
| `DeclaracaoProcedimento` | `getId()` e `getDefProcedimento()` — promovidos de `private` para `public` |

As classes de expressão (`ExpBinaria`, `ExpUnaria`, `Id`, valores) já tinham getters públicos e não precisaram ser tocadas. Com essa abertura, um visitor por `instanceof` consegue percorrer toda a árvore sem reflexão e sem acessar nenhum campo privado.

---

## Slide 9 — Fase 2: Validação

**Título:** Fase 2 — Testes de Precedência

**Texto:**

A gramática nova foi validada com sete programas curtos que exercitam exatamente os casos que antes davam errado. Os resultados confirmam que a árvore agora tem o significado correto:

| Programa | Saída | O que comprova |
|---|---|---|
| `1 + 2 + 3` | `6` | soma associativa |
| `1 - 2 - 3` | `-4` | associatividade à esquerda (antes dava `2`) |
| `-1 + 2` | `1` | unário não-guloso (antes dava `-3`) |
| `1 + 2 == 3` | `true` | `==` aplicado por último (antes: erro de tipo) |
| `true or false and false` | `true` | `and` com precedência maior que `or` |

Os três últimos casos são os mais significativos: só passam a funcionar com a gramática fatorada. Com a versão antiga, `1 + 2 == 3` nem chegava a executar.

---

## Slide 10 — Fase 3: Objetivo e Arquitetura

**Título:** Fase 3 — Estruturação da Análise Semântica

**Texto:**

A Fase 3 construiu a camada que dará suporte confiável ao linter. Ela acrescenta um passo novo no pipeline, entre o parser e a execução:

```text
Parser → Visitor Semântico → (se ok) checaTipo legado → execução
```

O visitor semântico percorre a AST construída pela Fase 2, preenche uma tabela de símbolos própria e acumula os erros encontrados. Se houver qualquer erro, o programa é reprovado e não chega a executar. É importante notar o que a Fase 3 **não** faz: ela não executa o programa e não emite avisos de qualidade — isso fica reservado para o linter da Fase 4.

---

## Slide 11 — Fase 3: A Tabela de Símbolos

**Título:** Fase 3 — Tabela de Símbolos Própria

**Texto:**

A análise semântica precisa de uma estrutura própria para registrar o que foi declarado. Optou-se por **não reaproveitar** o `Contexto<Tipo>` legado, porque ele só guarda o tipo e é descartado quando o escopo fecha. O analisador estático precisa de mais.

A nova tabela de símbolos é formada por três classes que trabalham juntas:

| Classe | Papel | O que guarda |
|---|---|---|
| `Simbolo` | representa uma entrada declarada | nome, tipo, *kind* (variável, parâmetro ou procedimento), nível do escopo, contadores `lido` e `escrito`, e — para procedimentos — a lista de parâmetros formais |
| `Escopo` | representa um escopo léxico | um `LinkedHashMap` de nome → símbolo e uma referência ao escopo pai |
| `TabelaSimbolos` | orquestra os escopos | o escopo atual, a lista de escopos já fechados e as operações `abrirEscopo`, `fecharEscopo`, `declarar` e `buscar` |

A descrição de cada peça vem nos próximos slides.

---

## Slide 12 — Fase 3: Como o Símbolo é Modelado

**Título:** Fase 3 — A Classe `Simbolo`

**Texto:**

Cada identificador declarado vira um `Simbolo`. O que torna essa classe diferente de um simples par "nome → tipo" são dois detalhes pensados desde já para o futuro:

O campo **`kind`** distingue variável, parâmetro e procedimento. Essa distinção é o que permite ao visitor recusar usos errados — por exemplo, um `call` aplicado a uma variável, ou um procedimento usado como se fosse um valor numa expressão.

Os campos **`lido`** e **`escrito`** são contadores. Toda vez que o visitor encontra um identificador sendo lido numa expressão, ele incrementa `lido`; toda vez que encontra uma atribuição ou um `read`, incrementa `escrito`. A Fase 3 ainda não usa esses números para nada, mas o linter da Fase 4 vai: uma variável com `lido` igual a zero é exatamente uma "variável declarada e nunca utilizada". Preencher isso agora evita ter que voltar nessa classe depois.

---

## Slide 13 — Fase 3: Como os Escopos Funcionam

**Título:** Fase 3 — Escopos e Resolução de Nomes

**Texto:**

A `TabelaSimbolos` começa com um **escopo global**, de nível 0. A partir daí, dois eventos abrem novos escopos: cada bloco `{ declaração ; comando }` abre um escopo local, e cada declaração de procedimento abre um escopo separado para o corpo, onde os parâmetros formais são registrados antes de visitar o comando interno.

A busca por um nome é **encadeada**: procura primeiro no escopo atual e, se não achar, sobe para o escopo pai, até a raiz. É isso que permite o *shadowing* — uma variável interna pode ter o mesmo nome de uma externa e sobrepô-la localmente.

Há uma decisão importante no fechamento de escopo: `fecharEscopo()` **não destrói** o escopo, apenas o move para uma lista de escopos fechados. O motivo é o linter — para avisar "esta variável de bloco nunca foi lida", a Fase 4 precisa conseguir inspecionar escopos que já saíram de cena. Guardar os símbolos em `LinkedHashMap` ainda garante que essa inspeção saia na ordem de declaração do código.

---

## Slide 14 — Fase 3: O Visitor Semântico

**Título:** Fase 3 — Visitor e Erros Padronizados

**Texto:**

O `VisitorSemantico` percorre a AST com despacho por `instanceof`, em três frentes: comandos, declarações e expressões. Para cada construção da linguagem, ele aplica uma regra — a condição de um `if` precisa ser booleana, uma atribuição precisa ter tipos compatíveis, uma chamada de procedimento precisa bater em aridade e em tipos de argumento, e assim por diante.

Um cuidado de projeto: o visitor **não para no primeiro erro**. Ele registra o problema numa lista e segue analisando, para que uma única execução mostre o máximo de erros possível. Para não gerar erros em cascata, uma sub-expressão com tipo indeterminado é marcada como "tipo desconhecido" e os erros seguintes daquela cadeia são suprimidos.

Todos os erros saem num formato uniforme, com um código que os categoriza:

```text
ERRO SEMANTICO [CODIGO]: mensagem
```

São nove códigos no total, cobrindo identificador não declarado, redeclaração, uso indevido de variável ou procedimento, incompatibilidade de tipo em atribuição, condição não booleana, operadores incompatíveis, aridade e tipo de argumento de procedimento.

---

## Slide 15 — Fase 3: Validação

**Título:** Fase 3 — Suite de Testes

**Texto:**

A análise semântica foi validada com 15 programas: 4 válidos, que devem passar e executar, e 11 inválidos, cada um disparando um código de erro específico. Os casos cobrem declaração antes do uso, compatibilidade de tipos, controle de escopo e chamadas de procedimento.

Dois testes merecem destaque. Um deles tem escopos aninhados com *shadowing* — uma variável `x` inteira no escopo externo e uma `x` booleana no interno — e confirma que a resolução de nomes funciona. O outro é um único programa propositalmente cheio de erros, usado para comprovar que o visitor acumula vários diagnósticos numa só execução:

```text
==== ERROS SEMANTICOS ====
ERRO SEMANTICO [TIPO_INCOMPATIVEL_ATRIBUICAO]: ...
ERRO SEMANTICO [IDENTIFICADOR_NAO_DECLARADO]: ...
ERRO SEMANTICO [TIPO_CONDICAO_NAO_BOOLEANO]: ...
ERRO SEMANTICO [IDENTIFICADOR_NAO_DECLARADO]: ...
Total: 4 erro(s) semantico(s). Execucao abortada.
```

Todos os 15 programas produziram exatamente a saída esperada.

---

## Slide 16 — Status e Próximos Passos

**Título:** Onde Estamos e o Que Vem

**Texto:**

As três primeiras fases estão concluídas e validadas:

- **Fase 1** — a base foi mapeada e as lacunas técnicas catalogadas.
- **Fase 2** — o parser passou a tratar precedência e associatividade corretamente, e a AST foi aberta para visitors.
- **Fase 3** — a análise semântica ganhou tabela de símbolos própria, visitor por `instanceof` e erros padronizados.

A próxima etapa é a **Fase 4 — o linter**, com três regras: variável declarada e nunca utilizada, código morto a partir de condições constantes, e complexidade por procedimento. O trabalho da Fase 3 já deixou o terreno pronto: a tabela de símbolos carrega os contadores de leitura e escrita que a regra de variável não utilizada vai consumir, e o ponto de integração no pipeline já está definido.
