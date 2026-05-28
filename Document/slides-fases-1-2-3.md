# Slides — Fases 1, 2 e 3

## Analisador Estático e Linter para a Linguagem Imperativa 2

> Formato de cada slide: do lado esquerdo, **bullets curtos** pra eu falar em cima (não é pra ler — é só pra eu não me perder). Do lado direito, um **trecho real de código** do nosso projeto pra mostrar onde aquilo vive.

---

## Slide 1 — Capa

**Título:** Analisador Estático e Linter para a Linguagem Imperativa 2

**Falar:**

- Partimos de uma base que já interpretava a Imperativa 2.
- A ideia foi *parar de só executar* e começar a **olhar pro código**: validar regras, apontar problemas, sugerir melhorias.
- Hoje eu apresento as três fases já concluídas — levantamento, preparação da AST e análise semântica. A quarta (o linter) já está em andamento em cima dessa base.

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
- O controle de escopo já vinha pronto em `Contexto<T>` — uma pilha de `HashMap` que atende muito bem o lado da execução. Pra análise estática, a gente ia complementar com uma estrutura própria.

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

## Slide 5 — Fase 1: Pontos de Extensão e Decisão de Arquitetura

**Título:** Fase 1 — O Que Iríamos Acrescentar

**Falar:**

- A base estava sólida pra o que ela se propunha — executar programas. Pra análise estática, a gente identificou três pontos onde ia precisar **estender**:
  1. **Expor a AST aos visitors** — acrescentar getters nos nós pra que um percorredor consiga descer pela árvore.
  2. **Criar a infraestrutura de análise** — visitor, tabela de símbolos própria e classe de erro semântico, tudo novo.
  3. **Refinar a gramática de expressões** — consolidar a precedência e os unários pra que a árvore reflita diretamente a semântica pretendida.
- **Decisão de arquitetura** tomada já nesta fase: o visitor seria implementado por **`instanceof`**, e não pelo padrão Visitor clássico com `accept()`.
- Por quê? Porque o `accept()` exigiria mexer em **todas** as classes da AST. Como temos um número pequeno e fechado de tipos de nó, `instanceof` é mais simples, suficiente e isolado num único arquivo.

**Código (lado direito):**

```java
// Ponto de extensão #1: expor os filhos pros visitors
public class IfThenElse implements Comando {
    private Expressao expressao;
    private Comando comandoThen;
    private Comando comandoElse;

    public AmbienteExecucaoImperativa executar(...) { ... }
    public boolean checaTipo(...) { ... }
    // Fase 2 acrescenta getExpressao(), getComandoThen(), getComandoElse()
}
```

---

## Slide 6 — Fase 2: Objetivo

**Título:** Fase 2 — Preparar a AST para a Análise

**Falar:**

- A Fase 2 tem **um objetivo central**: deixar a AST pronta pra ser percorrida por visitors.
- Em prática, isso quis dizer duas coisas:
  1. **Expor os nós** — acrescentar getters pros visitors conseguirem descer pela árvore.
  2. **Consolidar a gramática de expressões** — refinar a precedência e os unários pra que a árvore reflita diretamente a semântica pretendida.
- A parte da exposição dos nós é o **coração** desta fase — é o que viabiliza tudo que vem na Fase 3. O ajuste de gramática foi um trabalho menor, mais pontual.

*(sem código — slide de objetivo)*

---

## Slide 7 — Fase 2: Abertura da AST

**Título:** Fase 2 — AST Acessível aos Visitors

**Falar:**

- Pro visitor descer pela árvore, cada nó precisa **expor seus filhos**.
- A gente acrescentou getters públicos a **11 classes** da AST. As expressões já tinham getters, então foram aproveitadas como estavam.
- Com isso, o visitor por `instanceof` consegue percorrer a árvore inteira **sem reflexão e sem acessar campo privado**.
- É uma mudança simples no código, mas estratégica — é ela que viabiliza toda a análise da Fase 3.

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

## Slide 8 — Fase 2: Consolidação da Gramática

**Título:** Fase 2 — Consolidando a Gramática de Expressões

**Falar:**

- Em paralelo à exposição da AST, a gente **consolidou** a gramática de expressões.
- Em uma frase: a gramática foi **fatorada em níveis de precedência**, e os unários passaram a consumir só a primária à frente.
- É um detalhe técnico do JavaCC — o que importa pra apresentação é o **efeito prático**: a árvore passa a refletir diretamente a semântica pretendida das expressões, então a análise semântica da Fase 3 pode confiar 100% no que recebe.
- Foi um ajuste pontual, mas dá uma base muito mais firme pra todo o trabalho de análise que vem depois.

**Código (lado direito — só pra ilustrar):**

```javacc
// Imperative2.jj — gramática fatorada em níveis
// nivel 1: ==
// nivel 2: + - or ++   (assoc. à esquerda)
// nivel 3: and         (assoc. à esquerda)
// nivel 4: unários e primárias

Expressao PExpBinaria2() :
{ Expressao acc; Expressao dir; }
{
    acc = PExpBinaria3()
    ( <PLUS> dir = PExpBinaria3() { acc = new ExpSoma(acc, dir); }
    | ... )*
    { return acc; }
}
```

---

## Slide 9 — Fase 2: Validação

**Título:** Fase 2 — Como Validamos

**Falar:**

- A exposição da AST foi validada de forma prática: a Fase 3 inteira só funciona porque o visitor consegue descer pela árvore. É a prova viva de que o contrato ficou completo.
- A consolidação da gramática foi validada com programas curtos que exercitam os casos de precedência e associatividade — todos avaliam para o resultado esperado.
- O ponto: depois da Fase 2, a árvore está **acessível e consistente** — pré-requisito pra tudo que vem a seguir.

**Código (lado direito — só pra dar a ideia):**

```text
Entrada                    | Resultado
---------------------------+-----------
1 + 2 + 3                  |   6
1 + 2 == 3                 | true
-1 + 2                     |   1
true or false and false    | true
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

**Título:** Fase 3 — Uma Tabela Própria pra Análise Estática

**Falar:**

- A gente construiu uma tabela de símbolos própria, **complementar** ao `Contexto<Tipo>` existente.
- A ideia foi não interferir no que já funcionava: o `Contexto` segue cuidando da execução, e essa nova tabela cuida do que a análise estática precisa de extra — **kind** do símbolo, **contadores de uso** (lido/escrito), e **escopos que sobrevivem ao fechamento** pro linter conseguir inspecionar depois.
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

## Slide 14 — Fase 3: Visitor — Estratégia

**Título:** Fase 3 — O Visitor Semântico (1/2): Estratégia

**Falar:**

- **Despacho por `instanceof`** — escolha tomada lá na Fase 1. O método principal olha o tipo do nó e roteia pra um método específico (`visitarIfThenElse`, `visitarAtribuicao`, etc).
- O visitor trabalha em **três frentes**, espelhando a AST:
  - **Comandos** — `if`, `while`, atribuição, `read`, `write`, chamada de procedimento, sequência, bloco com declaração.
  - **Declarações** — variável, procedimento, composta.
  - **Expressões** — função `tipoDe()`, que devolve o tipo da expressão ou `null` se for indeterminado.
- Duas decisões de projeto importantes:
  1. **Não para no primeiro erro** — registra na lista `erros` e segue. Uma execução só, máximo de problemas reportados.
  2. **Tipo desconhecido (`null`) suprime cascata** — se uma sub-expressão deu erro, os checks que dependem dela são silenciados pra não soltar erro derivado.

**Código (lado direito):**

```java
public class VisitorSemantico {

    private final TabelaSimbolos tabela = new TabelaSimbolos();
    private final List<ErroSemantico> erros = new ArrayList<>();

    public List<ErroSemantico> analisar(Programa programa) {
        if (programa != null && programa.getComando() != null) {
            visitarComando(programa.getComando());
        }
        return Collections.unmodifiableList(erros);
    }

    private void visitarComando(Comando c) {
        if (c instanceof IfThenElse)          { visitarIfThenElse((IfThenElse) c); return; }
        if (c instanceof While)               { visitarWhile((While) c); return; }
        if (c instanceof Atribuicao)          { visitarAtribuicao((Atribuicao) c); return; }
        if (c instanceof ChamadaProcedimento) { visitarChamadaProcedimento(...); return; }
        if (c instanceof ComandoDeclaracao)   { visitarComandoDeclaracao(...); return; }
        if (c instanceof SequenciaComando)    { /* visita os dois lados */ return; }
        if (c instanceof Read)                { visitarRead((Read) c); return; }
        if (c instanceof Write)               { tipoDe(((Write) c).getExpressao()); return; }
    }
}
```

---

## Slide 15 — Fase 3: Visitor — Exemplos do Que Ele Checa

**Título:** Fase 3 — O Visitor Semântico (2/2): Em Ação

**Falar:**

- Cada método de visita aplica **uma ou mais regras** pro nó que recebeu. Dois exemplos concretos:
- **`visitarAtribuicao`** — quatro coisas em sequência:
  1. O identificador existe? (busca na tabela)
  2. É realmente uma variável? (não pode atribuir a procedimento)
  3. Os tipos batem? (variável e expressão precisam ter o mesmo tipo)
  4. Marca o símbolo como "escrito" — já alimentando o contador que o linter vai usar.
- **`visitarChamadaProcedimento`** — também checks em cascata:
  1. O nome existe?
  2. É um procedimento? (não pode "chamar" uma variável)
  3. A **aridade** bate? (número de argumentos)
  4. Os **tipos** dos argumentos batem com os dos parâmetros formais?
- Cada falha vira um `ErroSemantico` com um código específico — então o relatório fica categorizado.

**Código (lado direito):**

```java
private void visitarAtribuicao(Atribuicao c) {
    Id id = c.getId();
    Simbolo s = tabela.buscar(id.getIdName());
    Tipo tipoExp = tipoDe(c.getExpressao());

    if (s == null) {
        registrar(Codigo.IDENTIFICADOR_NAO_DECLARADO, ...);  return;
    }
    if (s.getKind() == SimboloKind.PROCEDIMENTO) {
        registrar(Codigo.IDENTIFICADOR_NAO_E_VARIAVEL, ...); return;
    }
    s.incrementarEscrito();              // ← alimenta o linter

    if (s.getTipo() != null && tipoExp != null
            && !s.getTipo().eIgual(tipoExp)) {
        registrar(Codigo.TIPO_INCOMPATIVEL_ATRIBUICAO, ...);
    }
}

private void visitarChamadaProcedimento(ChamadaProcedimento c) {
    Simbolo s = tabela.buscar(c.getNomeProcedimento().getIdName());
    if (s == null)                          { registrar(NAO_DECLARADO, ...);       return; }
    if (s.getKind() != PROCEDIMENTO)        { registrar(NAO_E_PROCEDIMENTO, ...);  return; }

    int aridadeFormal = ...;
    int aridadeReal   = ...;
    if (aridadeFormal != aridadeReal)       { registrar(ARIDADE_PROCEDIMENTO, ...); return; }

    // aridades batem: checa tipo de cada argumento contra cada parâmetro formal
    // → TIPO_ARGUMENTO_PROCEDIMENTO se algum não bater
}
```

---

## Slide 16 — Fase 3: Erros Padronizados

**Título:** Fase 3 — O Formato dos Erros

**Falar:**

- Todos os erros saem num **formato uniforme**, com um código que os categoriza.
- São **9 códigos** no total, cobrindo as principais classes de problema.
- Esse formato uniforme já é pensado pra Fase 4: os avisos do linter vão seguir o mesmo padrão, só prefixados com `LINT` em vez de `ERRO SEMANTICO`.

**Código (lado direito):**

```text
ERRO SEMANTICO [CODIGO]: mensagem
```

**Os 9 códigos:**

```text
IDENTIFICADOR_NAO_DECLARADO        TIPO_INCOMPATIVEL_ATRIBUICAO
IDENTIFICADOR_JA_DECLARADO         TIPO_CONDICAO_NAO_BOOLEANO
IDENTIFICADOR_NAO_E_VARIAVEL       TIPO_OPERADOR_INCOMPATIVEL
IDENTIFICADOR_NAO_E_PROCEDIMENTO   ARIDADE_PROCEDIMENTO
                                   TIPO_ARGUMENTO_PROCEDIMENTO
```

---

## Slide 17 — Fase 3: Validação

**Título:** Fase 3 — Suite de Testes

**Falar:**

- **15 programas**: 4 válidos (que devem passar e executar) + 11 inválidos (cada um disparando um código específico).
- Cobertura: declaração antes do uso, tipos, escopo, chamadas de procedimento.
- Dois testes que vale destacar:
  - Um com **escopos aninhados e shadowing** — `x` inteira por fora, `x` booleana por dentro. Confirma que a resolução de nomes funciona.
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

## Slide 18 — Status e Próximos Passos

**Título:** Onde Estamos e o Que Vem

**Falar:**

- **Fase 1** — base mapeada, pontos de extensão identificados, decisão pelo visitor por `instanceof`.
- **Fase 2** — AST exposta pros visitors + gramática de expressões consolidada.
- **Fase 3** — visitor semântico em pé, tabela de símbolos própria, erros padronizados.
- A próxima é a **Fase 4 — o linter**, com três regras:
  1. Variável declarada e nunca utilizada.
  2. Código morto a partir de condição constante.
  3. Complexidade por procedimento.
- O terreno já está pronto: os contadores `lido`/`escrito` já estão sendo preenchidos, o `kind` distingue os símbolos, os escopos fechados ficam arquivados, e o ponto de integração no pipeline já está definido.

*(sem código — slide de fechamento)*
