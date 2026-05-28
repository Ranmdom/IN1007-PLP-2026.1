# Slides — Fases 1, 2 e 3

## Analisador Estático e Linter para a Linguagem Imperativa 2

> Formato de cada slide: do lado esquerdo, **bullets curtos** pra eu falar em cima (não é pra ler — é só pra eu não me perder). Do lado direito, um **trecho real de código** do nosso projeto pra mostrar onde aquilo vive.

---

## Slide 1 — Capa

**Título:** Analisador Estático e Linter para a Linguagem Imperativa 2

**Falar:**

- Partimos de uma base que já interpretava a Imperativa 2.
- A ideia foi *parar de só executar* e começar a **olhar pro código**: validar regras, apontar problemas, sugerir melhorias.
- Hoje eu apresento as três fases já concluídas — levantamento, gramática/AST e análise semântica. A quarta (o linter) já está em andamento em cima dessa base.

*(sem código nesse slide — capa)*

---

## Slide 2 — Visão Geral

**Título:** O Pipeline da Ferramenta

**Falar:**

- Tudo é organizado como um pipeline — cada etapa entrega pra próxima.
- A decisão de projeto mais importante: **separar "errado" de "ruim"**. Parser só reconhece. Semântica diz se é válido. Linter diz se é de boa qualidade.
- Isso evita misturar diagnóstico de erro com diagnóstico de estilo, que têm naturezas diferentes.

**Código (lado direito):**

```text
Código-fonte
  ↓ JavaCC (Imperative2.jj)
Análise léxica + sintática
  ↓
        AST
  ↓
Análise semântica   
  ↓
Análise de linter  
  ↓
Relatório de erros e avisos
```

---

## Slide 3 — Fase 1: Objetivo

**Título:** Fase 1 — Levantar e Entender a Base

**Falar:**

- Antes de escrever uma linha de código novo, a gente precisava entender o que já existia.
- Quatro perguntas guiaram essa fase:
  1. Onde a AST é construída?
  2. Como a execução atual consome essa AST?
  3. Onde a gente encaixa a análise semântica?
  4. Onde a gente encaixa o linter?
- O resultado dessa fase **não é código** — é conhecimento. Um mapa do projeto e uma lista das lacunas que iam travar a gente depois.

*(sem código — slide de objetivo)*

---

## Slide 4 — Fase 1: O Que Foi Mapeado

**Título:** Fase 1 — Mapa da Base de Código

**Falar:**

- O parser `Imperative2.jj` constrói tudo a partir da classe raiz `Programa`.
- A AST já tinha nó pra praticamente toda a linguagem — comandos (`If`, `While`, `Atribuicao`, `Write`, `Read`, `Sequencia`, `ComandoDeclaracao`, `ChamadaProcedimento`), declarações (variável, composta, procedimento) e expressões.
- O controle de escopo já existia em `Contexto<T>` — uma pilha de `HashMap`. Mas era pensado pra execução, não pra análise estática.

**Código (lado direito):**

```text
src/li2/plp/imperative2/
├── parser/
│   └── Imperative2.jj         ← parser JavaCC
├── Programa.java              ← raiz da AST
├── command/                   ← IfThenElse, While, Atribuicao...
├── declaration/               ← DeclaracaoVariavel, DeclaracaoProcedimento...
└── expressions2/              ← ExpBinaria, ExpUnaria, Id, ValorInteiro...

expressions2/memory/Contexto.java   ← escopo legado (pilha de HashMap)
```

---

## Slide 5 — Fase 1: As Lacunas

**Título:** Fase 1 — O Que Precisava Ser Resolvido

**Falar:**

- Três obstáculos concretos pra avançar:
  1. **AST opaca** — os campos eram `private` sem getter. Nenhum visitor conseguia descer pela árvore.
  2. **Parser sem precedência** — todos os operadores binários no mesmo nível, unários "gulosos".
  3. **Sem infraestrutura de análise** — nem visitor, nem tabela de símbolos própria, nem classe de erro.
- Decisão de arquitetura tomada aqui: o visitor seria feito por `instanceof`, não pelo padrão Visitor clássico com `accept()`. Isso evita ter que mexer em todas as classes da AST.

**Código (lado direito):**

```java
// Como estava: AST "fechada", visitor não passa
public class IfThenElse implements Comando {
    private Expressao expressao;          // sem getter
    private Comando comandoThen;          // sem getter
    private Comando comandoElse;          // sem getter

    public AmbienteExecucaoImperativa executar(...) { ... }
    public boolean checaTipo(...) { ... }
    // nada além de executar/checaTipo
}
```

---

## Slide 6 — Fase 2: O Problema na Gramática

**Título:** Fase 2 — Precedência e Associatividade Quebradas

**Falar:**

- Na versão antiga, **todos os operadores binários ficavam no mesmo nível**, e a recursão era à direita.
- Ou seja: o parser aceitava o programa, mas a árvore tinha o **significado errado**.
- Exemplos do que dava errado:
  - `1 + 2 == 3` virava `1 + (2 == 3)` — erro de tipo.
  - `1 - 2 - 3` virava `1 - (2 - 3)` = `2`, em vez de `-4`.
  - `-x + 1` virava `-(x + 1)` por causa do unário guloso.
  - `or` e `and` no mesmo nível — sem distinguir prioridade.

**Código (lado direito):**

```text
Antes (mesmo nível, recursão à direita):

  ExpBinaria ::= ExpUnaria [ OP ExpBinaria ]
  OP         ::= + | - | == | or | and | ++

Resultado da árvore para "1 + 2 == 3":

        ==
       /  \
      1    +
          / \
         2   3       ← errado: tipo
```

---

## Slide 7 — Fase 2: Gramática em 3 Níveis

**Título:** Fase 2 — A Solução: Fatorar por Precedência

**Falar:**

- A gente reescreveu o trecho de expressões em **três níveis**: o mais fraco encosta no mais forte.
- Cada nível só monta seu nó depois de resolver o nível mais forte — isso garante precedência.
- Os níveis 2 e 3 usam **laço com acumulador** em vez de recursão à direita — assim a associatividade fica à esquerda.
- Os unários (`-`, `not`, `length`) passaram a chamar **só a primária** à frente — acabou o "gulodice".

**Código (lado direito — `Imperative2.jj`):**

```javacc
// nivel 1: ==  (não associativo)
Expressao PExpBinaria() :
{ Expressao esq; Expressao dir; }
{
    esq = PExpBinaria2()
    [ <EQ> dir = PExpBinaria2()
      { esq = new ExpEquals(esq, dir); } ]
    { return esq; }
}

// nivel 2: + - or ++  (assoc. à esquerda via loop)
Expressao PExpBinaria2() :
{ Expressao acc; Expressao dir; }
{
    acc = PExpBinaria3()
    (
        <PLUS>  dir = PExpBinaria3() { acc = new ExpSoma(acc, dir); }
      | <MINUS> dir = PExpBinaria3() { acc = new ExpSub(acc, dir); }
      | <OR>    dir = PExpBinaria3() { acc = new ExpOr(acc, dir); }
      | <CONCAT>dir = PExpBinaria3() { acc = new ExpConcat(acc, dir); }
    )*
    { return acc; }
}

// nivel 3: and  (assoc. à esquerda)
Expressao PExpBinaria3() :
{ Expressao acc; Expressao dir; }
{
    acc = PExpUnaria()
    ( <AND> dir = PExpUnaria() { acc = new ExpAnd(acc, dir); } )*
    { return acc; }
}

// nivel 4: unários chamam PRIMÁRIA, não PExpressao
Expressao PExpMenos() :
{ Expressao retorno; }
{ <MINUS> retorno = PExpPrimaria()
  { return new ExpMenos(retorno); } }
```

---

## Slide 8 — Fase 2: Abrindo a AST

**Título:** Fase 2 — AST Acessível aos Visitors

**Falar:**

- Pro visitor descer pela árvore, cada nó precisa **expor seus filhos**.
- A gente adicionou getters públicos a **11 classes** da AST — todas que estavam fechadas.
- As expressões já tinham getters — não precisaram ser tocadas.
- Com isso, o visitor por `instanceof` consegue percorrer a árvore inteira sem reflexão e sem acessar campo privado.

**Código (lado direito):**

```java
// IfThenElse.java — depois da Fase 2
public class IfThenElse implements Comando {
    private Expressao expressao;
    private Comando comandoThen;
    private Comando comandoElse;

    // executar() e checaTipo() continuam aqui

    public Expressao getExpressao()  { return expressao; }
    public Comando   getComandoThen(){ return comandoThen; }
    public Comando   getComandoElse(){ return comandoElse; }
}
```

| Classe | Getters adicionados |
|---|---|
| `Programa` | `getComando()` |
| `IfThenElse` | `getExpressao`, `getComandoThen`, `getComandoElse` |
| `While` | `getExpressao`, `getComando` |
| `Atribuicao` | `getId`, `getExpressao` |
| `Write` / `Read` | `getExpressao` / `getId` |
| `SequenciaComando` | `getComando1`, `getComando2` |
| `ComandoDeclaracao` | `getDeclaracao`, `getComando` |
| `ChamadaProcedimento` | `getNomeProcedimento`, `getParametrosReais` |
| `DeclaracaoComposta` | `getDeclaracao1`, `getDeclaracao2` |
| `DeclaracaoProcedimento` | `getId`, `getDefProcedimento` (private → public) |

---

## Slide 9 — Fase 2: Validação

**Título:** Fase 2 — Testes de Precedência

**Falar:**

- Sete programas curtos exercitando exatamente os casos que antes davam errado.
- Os três últimos são os mais significativos — antes nem rodavam.
- A árvore agora tem o **significado certo**.

**Código (lado direito):**

```text
Entrada                       | Saída  | Comprova
------------------------------+--------+-------------------------
1 + 2 + 3                     |    6   | soma associativa
1 - 2 - 3                     |   -4   | assoc. à esquerda
                              |        | (antes dava 2)
-1 + 2                        |    1   | unário não-guloso
                              |        | (antes dava -3)
1 + 2 == 3                    |  true  | == aplicado por último
                              |        | (antes: erro de tipo)
true or false and false       |  true  | and tem prioridade > or
```

---

## Slide 10 — Fase 3: Arquitetura

**Título:** Fase 3 — Estruturação da Análise Semântica

**Falar:**

- Aqui a gente acrescenta um **passo novo no pipeline**, entre o parser e a execução.
- O visitor semântico percorre a AST (já aberta pela Fase 2), preenche uma tabela de símbolos própria e **acumula erros**.
- Se aparecer erro, o programa é reprovado e nem chega a executar.
- O que essa fase **não** faz: não executa o programa e não gera aviso de qualidade — isso é o linter da Fase 4.

**Código (lado direito):**

```text
Parser
  ↓
Visitor Semântico   ← Fase 3
  ↓
  ├─ se OK → checaTipo legado → execução
  └─ se erro → relatório, aborta
```

---

## Slide 11 — Fase 3: Tabela de Símbolos

**Título:** Fase 3 — Por Que Uma Tabela Nova

**Falar:**

- A gente **decidiu não reaproveitar** o `Contexto<Tipo>` legado.
- Motivo: ele só guarda o tipo e é descartado quando o escopo fecha. A análise estática (e o linter depois) precisa de mais.
- Três classes formam a tabela:
  - `Simbolo` — entrada declarada (nome, tipo, kind, contadores).
  - `Escopo` — `LinkedHashMap` + ponteiro pro pai.
  - `TabelaSimbolos` — orquestra os escopos (abrir, fechar, declarar, buscar).

**Código (lado direito):**

```java
public class TabelaSimbolos {
    private Escopo escopoAtual;
    private final List<Escopo> escoposFechados = new ArrayList<>();
    private int proximoNivel = 0;

    public void abrirEscopo() {
        this.escopoAtual = new Escopo(proximoNivel++, escopoAtual);
    }

    public void fecharEscopo() {
        if (escopoAtual.getPai() == null) {
            throw new IllegalStateException(
                "tentativa de fechar o escopo global");
        }
        escoposFechados.add(escopoAtual);   // ← não destrói: arquiva
        escopoAtual = escopoAtual.getPai();
    }

    public Simbolo buscar(String nome) {
        Escopo e = escopoAtual;
        while (e != null) {
            Simbolo s = e.buscarLocal(nome);
            if (s != null) return s;
            e = e.getPai();                  // ← sobe a cadeia
        }
        return null;
    }
}
```

---

## Slide 12 — Fase 3: A Classe `Simbolo`

**Título:** Fase 3 — Como Modelamos um Símbolo

**Falar:**

- Cada identificador declarado vira um `Simbolo`. Dois detalhes pensados desde já pro futuro:
- O `kind` distingue **variável, parâmetro e procedimento** — é isso que permite recusar usos errados (tipo `call` em variável, ou procedimento usado como expressão).
- Os contadores `lido` / `escrito` ainda não são usados nessa fase. Foram colocados agora pra evitar voltar nessa classe na Fase 4. Quando o linter rodar, "variável declarada e nunca utilizada" vira só `lido == 0`.

**Código (lado direito):**

```java
public class Simbolo {
    private final String nome;
    private final Tipo tipo;
    private final SimboloKind kind;       // VARIAVEL | PARAMETRO | PROCEDIMENTO
    private final int escopoNivel;
    private final ListaDeclaracaoParametro parametrosFormais;

    private int lido = 0;
    private int escrito = 0;

    public void incrementarLido()    { lido++; }
    public void incrementarEscrito() { escrito++; }

    // getters...
}
```

---

## Slide 13 — Fase 3: Escopos e Resolução de Nomes

**Título:** Fase 3 — Como os Escopos Funcionam

**Falar:**

- A tabela começa com o **escopo global**, nível 0.
- Dois eventos abrem escopo novo:
  1. Cada bloco `{ declaração ; comando }`.
  2. Cada declaração de procedimento — pra registrar os parâmetros formais antes do corpo.
- Busca é **encadeada**: escopo atual → pai → ... → raiz. É isso que permite **shadowing**.
- Decisão importante: `fecharEscopo()` **não destrói** o escopo. Arquiva. O linter vai precisar inspecionar escopos que já saíram de cena (pra avisar "esta variável de bloco nunca foi lida").

**Código (lado direito):**

```java
public class Escopo {
    private final int nivel;
    private final Escopo pai;
    private final Map<String, Simbolo> simbolos = new LinkedHashMap<>();
    //                                          ↑ ordem de declaração

    public boolean declarar(Simbolo simbolo) {
        if (simbolos.containsKey(simbolo.getNome())) {
            return false;                 // ← redeclaração no mesmo escopo
        }
        simbolos.put(simbolo.getNome(), simbolo);
        return true;
    }

    public Simbolo buscarLocal(String nome) {
        return simbolos.get(nome);        // ← não sobe pro pai
    }
}
```

---

## Slide 14 — Fase 3: O Visitor Semântico

**Título:** Fase 3 — Visitor e Erros Padronizados

**Falar:**

- `VisitorSemantico` despacha por `instanceof` em três frentes: **comandos, declarações e expressões**.
- Cuidado importante: ele **não para no primeiro erro**. Registra na lista `erros` e segue analisando — uma execução só, máximo de erros possível.
- Pra não cascatear: sub-expressão com tipo indeterminado vira "tipo desconhecido" e os erros seguintes daquela cadeia são suprimidos.
- Saída uniforme: `ERRO SEMANTICO [CODIGO]: mensagem`. **9 códigos** no total.

**Código (lado direito):**

```java
private void visitarComando(Comando c) {
    if (c instanceof IfThenElse)         { visitarIfThenElse((IfThenElse) c); return; }
    if (c instanceof While)              { visitarWhile((While) c); return; }
    if (c instanceof Atribuicao)         { visitarAtribuicao((Atribuicao) c); return; }
    if (c instanceof ChamadaProcedimento){ visitarChamadaProcedimento(...); return; }
    // ...
}

private void visitarAtribuicao(Atribuicao c) {
    Id id = c.getId();
    Simbolo s = tabela.buscar(id.getIdName());
    Tipo tipoExp = tipoDe(c.getExpressao());

    if (s == null) {
        registrar(Codigo.IDENTIFICADOR_NAO_DECLARADO,
            "identificador '" + id.getIdName() + "' nao declarado");
        return;
    }
    if (s.getKind() == SimboloKind.PROCEDIMENTO) {
        registrar(Codigo.IDENTIFICADOR_NAO_E_VARIAVEL, ...);
        return;
    }
    s.incrementarEscrito();               // ← já alimentando o linter

    if (s.getTipo() != null && tipoExp != null
            && !s.getTipo().eIgual(tipoExp)) {
        registrar(Codigo.TIPO_INCOMPATIVEL_ATRIBUICAO, ...);
    }
}
```

**Códigos de erro:**

```text
IDENTIFICADOR_NAO_DECLARADO        TIPO_INCOMPATIVEL_ATRIBUICAO
IDENTIFICADOR_JA_DECLARADO         TIPO_CONDICAO_NAO_BOOLEANO
IDENTIFICADOR_NAO_E_VARIAVEL       TIPO_OPERADOR_INCOMPATIVEL
IDENTIFICADOR_NAO_E_PROCEDIMENTO   ARIDADE_PROCEDIMENTO
                                   TIPO_ARGUMENTO_PROCEDIMENTO
```

---

## Slide 15 — Fase 3: Validação

**Título:** Fase 3 — Suite de Testes

**Falar:**

- **15 programas**: 4 válidos (que devem passar e executar) + 11 inválidos (cada um dispara um código específico).
- Cobertura: declaração antes do uso, tipos, escopo, chamadas de procedimento.
- Dois testes que vale destacar:
  - Um com **escopos aninhados e shadowing** — `x` inteira por fora, `x` booleana por dentro. Confirma que a resolução funciona.
  - Um programa propositalmente cheio de erros — prova que o visitor **acumula** vários diagnósticos numa execução só.

**Código (lado direito — saída real):**

```text
==== ERROS SEMANTICOS ====
ERRO SEMANTICO [TIPO_INCOMPATIVEL_ATRIBUICAO]: ...
ERRO SEMANTICO [IDENTIFICADOR_NAO_DECLARADO]: ...
ERRO SEMANTICO [TIPO_CONDICAO_NAO_BOOLEANO]: ...
ERRO SEMANTICO [IDENTIFICADOR_NAO_DECLARADO]: ...
Total: 4 erro(s) semantico(s). Execucao abortada.
```

Os 15 programas produziram exatamente a saída esperada.

---

## Slide 16 — Status e Próximos Passos

**Título:** Onde Estamos e o Que Vem

**Falar:**

- **Fase 1** — base mapeada, lacunas catalogadas.
- **Fase 2** — parser com precedência correta + AST aberta pros visitors.
- **Fase 3** — tabela de símbolos própria, visitor por `instanceof`, erros padronizados.
- A próxima é a **Fase 4 — o linter**, com três regras:
  1. Variável declarada e nunca utilizada.
  2. Código morto a partir de condição constante.
  3. Complexidade por procedimento.
- O terreno já está pronto: os contadores `lido`/`escrito` já estão sendo preenchidos, e o ponto de integração no pipeline já está definido.

*(sem código — slide de fechamento)*
