# Arvore Sintatica, Tabela de Simbolos e Visitors

## Linguagem Imperativa 2

Este documento consolida tres artefatos tecnicos que apoiam as Fases 2, 3 e 4 do [plano de implementacao](plano-implementacao-linter-imperativa2.md):

- a arvore de tipos da AST construida pelo parser `Imp2Parser`
- um exemplo concreto de tabela de simbolos para um programa pequeno
- a proposta de onde os visitors de analise semantica e de linter se encaixam sobre a AST existente

O objetivo e servir como referencia rapida durante a implementacao do analisador estatico, sem precisar caminhar pelo codigo-fonte a cada decisao.

## 1. Arvore de Tipos da AST

A AST da Imperativa 2 e composta por tres grandes hierarquias, ancoradas por interfaces: `Comando`, `Declaracao` e `Expressao`. A raiz do programa e sempre um [Programa.java](PLP_2026/Imperativa2/src/li2/plp/imperative2/Programa.java) que encapsula um `Comando`.

### 1.1. Visao em arvore de classes

```text
Programa
  └── Comando (interface)
        ├── Skip
        ├── Atribuicao              (Id, Expressao)
        ├── SequenciaComando        (Comando, Comando)
        ├── IfThenElse              (Expressao, Comando, Comando)
        ├── While                   (Expressao, Comando)
        ├── IO (interface)
        │     ├── Read              (Id)
        │     └── Write             (Expressao)
        ├── ComandoDeclaracao       (Declaracao, Comando)
        └── ChamadaProcedimento     (Id, ListaExpressao)

Declaracao (abstract)
  ├── DeclaracaoVariavel            (Id, Expressao)
  ├── DeclaracaoComposta            (Declaracao, Declaracao)
  └── DeclaracaoProcedimento        (Id, DefProcedimento)
        └── DefProcedimento         (ListaDeclaracaoParametro, Comando)
              └── DeclaracaoParametro  (Id, Tipo)

Expressao (interface)
  ├── Valor (interface)
  │     ├── ValorInteiro            (int)
  │     ├── ValorBooleano           (boolean)
  │     └── ValorString             (String)
  ├── Id                            (String nome)
  ├── ExpUnaria (abstract)
  │     ├── ExpMenos                (Expressao)
  │     ├── ExpNot                  (Expressao)
  │     └── ExpLength               (Expressao)
  └── ExpBinaria (abstract)
        ├── ExpSoma                 (Expressao, Expressao)
        ├── ExpSub                  (Expressao, Expressao)
        ├── ExpAnd                  (Expressao, Expressao)
        ├── ExpOr                   (Expressao, Expressao)
        ├── ExpEquals               (Expressao, Expressao)
        └── ExpConcat               (Expressao, Expressao)

Listas auxiliares
  ├── ListaExpressao                (Expressao, ListaExpressao)
  └── ListaDeclaracaoParametro      (DeclaracaoParametro, ListaDeclaracaoParametro)

Tipos (usados pela analise semantica, nao sao nos de AST)
  ├── TipoPrimitivo                 (INTEIRO | BOOLEANO | STRING)
  └── TipoProcedimento              (assinatura com lista de Tipo)
```

### 1.2. Metodos que cada no ja expoe

As interfaces existentes ja oferecem dois pontos de entrada para a travessia. A analise estatica vai estender ou duplicar essa travessia sem alterar a semantica atual.

```java
public interface Expressao {
    Valor avaliar(AmbienteExecucao amb);
    boolean checaTipo(AmbienteCompilacao amb);
    Tipo    getTipo(AmbienteCompilacao amb);
    Expressao reduzir(AmbienteExecucao amb);
    Expressao clone();
}

public interface Comando {
    AmbienteExecucaoImperativa executar(AmbienteExecucaoImperativa amb);
    boolean checaTipo(AmbienteCompilacaoImperativa amb);
}

public abstract class Declaracao {
    public abstract AmbienteExecucaoImperativa elabora(AmbienteExecucaoImperativa amb);
    public abstract boolean checaTipo(AmbienteCompilacaoImperativa amb);
}
```

Observacao importante: hoje nao existe metodo `accept(Visitor v)`. A logica de tipo e execucao esta embutida dentro de cada no. A Secao 3 explica como encaixar visitors sem precisar reescrever essa estrutura.

### 1.3. Arvore concreta de um programa exemplo

Programa de referencia:

```text
{
  var x = 1 + 2,
  proc dobro(int n) {
    write(n + n)
  };
  call dobro(x)
}
```

A AST gerada pelo parser para esse programa e:

```text
Programa
└── ComandoDeclaracao
      ├── DeclaracaoComposta
      │     ├── DeclaracaoVariavel
      │     │     ├── Id("x")
      │     │     └── ExpSoma
      │     │           ├── ValorInteiro(1)
      │     │           └── ValorInteiro(2)
      │     └── DeclaracaoProcedimento
      │           ├── Id("dobro")
      │           └── DefProcedimento
      │                 ├── ListaDeclaracaoParametro
      │                 │     └── DeclaracaoParametro
      │                 │           ├── Id("n")
      │                 │           └── TipoPrimitivo.INTEIRO
      │                 └── Write
      │                       └── ExpSoma
      │                             ├── Id("n")
      │                             └── Id("n")
      └── ChamadaProcedimento
            ├── Id("dobro")
            └── ListaExpressao
                  └── Id("x")
```

Essa e a arvore que qualquer visitor da analise semantica ou do linter vai percorrer.

## 2. Tabela de Simbolos

A tabela de simbolos na base atual e implementada como pilha de `HashMap<Id, T>` dentro do pacote `expressions2/memory`. O tipo `T` muda conforme o estagio:

| Classe                           | Pilha de          | Usada em                      |
| -------------------------------- | ----------------- | ----------------------------- |
| `ContextoCompilacao`             | `Tipo`            | `checaTipo()` das expressoes  |
| `ContextoCompilacaoImperativa`   | `Tipo`            | `checaTipo()` dos comandos    |
| `ContextoExecucao`               | `Valor`           | `avaliar()` das expressoes    |
| `ContextoExecucaoImperativa`     | `Valor`           | `executar()` dos comandos     |
| `ContextoExecucaoImperativa2`    | `Valor` + `proc`  | chamadas de procedimento      |

A API central e sempre a mesma:

```java
interface Ambiente<T> {
    void incrementa();          // abre novo escopo
    void restaura();            // fecha escopo corrente
    void map(Id id, T valor);   // declara
    T    get(Id id);            // busca encadeando escopos
}
```

### 2.1. Modelo de Simbolo proposto para o linter

A base atual guarda apenas o tipo. O linter precisa de metadados adicionais (se foi lida, se foi parametro, se ja foi escrita). A Fase 3 do plano introduz uma nova estrutura de simbolo que envolve `Tipo`:

```java
class Simbolo {
    Id      nome;
    Tipo    tipo;
    Kind    kind;        // VARIAVEL | PARAMETRO | PROCEDIMENTO
    boolean foiLido;
    boolean foiEscrito;
    int     escopoNivel;
}
```

Essa estrutura vive dentro de um `AmbienteAnalise` proprio dos visitors, paralelo ao `ContextoCompilacao` existente. Assim, a analise nova nao interfere na execucao.

### 2.2. Exemplo sobre o programa da Secao 1.3

Quando o `SemanticVisitor` estiver processando o corpo do procedimento `dobro`, imediatamente apos entrar no escopo do parametro `n`, as pilhas de escopo estarao assim:

```text
topo da pilha ─────────────────────────────────────────────
Escopo 2  (corpo de 'dobro')
    n        -> Simbolo { tipo=int,    kind=PARAMETRO,  foiLido=false, foiEscrito=false, nivel=2 }
----------------------------------------------------------
Escopo 1  (bloco ComandoDeclaracao)
    x        -> Simbolo { tipo=int,    kind=VARIAVEL,   foiLido=false, foiEscrito=true,  nivel=1 }
    dobro    -> Simbolo { tipo=proc(int), kind=PROCEDIMENTO,                           nivel=1 }
----------------------------------------------------------
Escopo 0  (global do Programa)
    (vazio para este exemplo)
base da pilha ─────────────────────────────────────────────
```

Apos visitar a expressao `n + n` dentro do `write`, o simbolo `n` e marcado como `foiLido = true`. Ao sair do escopo 2 (fim do `DefProcedimento`), o linter verifica cada simbolo do topo:

- `n.foiLido == true` → nenhum aviso
- o escopo e fechado e descartado

Ao sair do escopo 1 (apos a `ChamadaProcedimento`), o simbolo `x` tera sido marcado `foiLido = true` porque foi usado como argumento. O procedimento `dobro` tera sido marcado como chamado. Nenhum aviso de variavel nao utilizada e emitido.

Se, por exemplo, a declaracao `var x = 1 + 2` nunca fosse lida, ao fechar o escopo 1 o linter emitiria:

```text
LINT: variavel 'x' declarada, mas nunca utilizada.
```

## 3. Onde os Visitors se Encaixam

A base atual nao usa o padrao Visitor classico. Toda logica e metodo de instancia dentro do no. Para introduzir a analise estatica sem quebrar a execucao, a proposta e adicionar visitors externos que percorrem a mesma AST por despacho por tipo (`instanceof`) ou por adicao de um `accept()` minimo nas interfaces.

### 3.1. Pipeline com visitors adicionados

```text
Codigo-fonte
  -> Imp2Parser.Input()                  [JavaCC, ja existe]
  -> Programa                            [raiz da AST]
        |
        +---> SemanticVisitor            [NOVO - Fase 3]
        |       . valida escopo
        |       . valida tipos
        |       . valida chamadas
        |       . produz List<ErroSemantico>
        |
        +---> LinterVisitor              [NOVO - Fase 4]
        |       . variaveis nao utilizadas
        |       . codigo morto
        |       . complexidade de proc
        |       . produz List<LintWarning>
        |
        +---> executar(...)              [ja existe, opcional no pipeline do linter]
```

Regra: o `LinterVisitor` so roda se o `SemanticVisitor` nao produziu erros graves. Isso evita avisos de linter sobre um programa que nao passa nem na checagem semantica basica.

### 3.2. Duas formas de introduzir o visitor

**Opcao A - visitor externo sem tocar na AST.** Cria-se uma unica classe `SemanticVisitor` que recebe o no raiz e usa `instanceof` para dispatch. Nao altera nenhum arquivo existente da AST.

```java
public class SemanticVisitor {
    private final AmbienteAnalise amb = new AmbienteAnalise();
    private final List<Diagnostico> erros = new ArrayList<>();

    public void visitar(Comando c) {
        if (c instanceof Skip)                  visitSkip((Skip) c);
        else if (c instanceof Atribuicao)       visitAtribuicao((Atribuicao) c);
        else if (c instanceof SequenciaComando) visitSequencia((SequenciaComando) c);
        else if (c instanceof IfThenElse)       visitIf((IfThenElse) c);
        else if (c instanceof While)            visitWhile((While) c);
        else if (c instanceof Write)            visitWrite((Write) c);
        else if (c instanceof Read)             visitRead((Read) c);
        else if (c instanceof ComandoDeclaracao) visitBloco((ComandoDeclaracao) c);
        else if (c instanceof ChamadaProcedimento) visitChamada((ChamadaProcedimento) c);
    }
    // ...
}
```

Vantagem: isolamento total. Desvantagem: cada novo tipo de no exige atualizar o dispatch manual.

**Opcao B - accept() minimo nas interfaces.** Adiciona-se um metodo `accept(AstVisitor v)` em `Comando`, `Declaracao` e `Expressao`, e cada no delega para o metodo correto do visitor.

```java
public interface AstVisitor {
    void visit(Atribuicao n);
    void visit(IfThenElse n);
    void visit(While n);
    void visit(Write n);
    // ... um metodo por no concreto
}

public interface Comando {
    AmbienteExecucaoImperativa executar(AmbienteExecucaoImperativa amb);
    boolean checaTipo(AmbienteCompilacaoImperativa amb);
    void accept(AstVisitor v);   // NOVO
}

public class Atribuicao implements Comando {
    // ...
    public void accept(AstVisitor v) { v.visit(this); }
}
```

Vantagem: dispatch estatico, compilador avisa quando falta tratar um no. Desvantagem: cada arquivo da AST ganha um metodo novo.

**Recomendacao:** comecar pela Opcao A. O numero de nos e pequeno (cerca de 25) e o acoplamento com a base existente fica minimo, o que facilita a defesa do trabalho como camada puramente aditiva.

### 3.3. Encaixe por no e por regra

A tabela abaixo resume qual visitor age em qual no, e qual regra do linter ele alimenta.

| No da AST                  | SemanticVisitor faz                                   | LinterVisitor faz                                             |
| -------------------------- | ----------------------------------------------------- | ------------------------------------------------------------- |
| `Programa`                 | abre escopo global                                    | inicia coleta de avisos                                       |
| `ComandoDeclaracao`        | `amb.incrementa()` antes, `amb.restaura()` depois     | ao fechar escopo, checa variaveis nao lidas                   |
| `DeclaracaoVariavel`       | registra `Simbolo(VARIAVEL)` com tipo da expressao    | marca `foiEscrito=true`                                       |
| `DeclaracaoComposta`       | visita `d1` e `d2` na ordem                           | nenhum                                                        |
| `DeclaracaoProcedimento`   | registra `Simbolo(PROCEDIMENTO)` com `TipoProcedimento` | entra em modo `procedimento atual` e zera contador de complexidade |
| `DefProcedimento`          | abre escopo, registra parametros, visita corpo        | ao sair, compara contador de complexidade com limiar          |
| `DeclaracaoParametro`      | registra `Simbolo(PARAMETRO)`                         | nenhum                                                        |
| `Atribuicao`               | valida tipo do lado esq e dir                         | marca `foiEscrito=true` do simbolo                            |
| `IfThenElse`               | valida que condicao e booleana                        | incrementa complexidade; detecta condicao constante (codigo morto) |
| `While`                    | valida que condicao e booleana                        | incrementa complexidade; detecta `while false`                |
| `Read`                     | valida que o id foi declarado                         | marca `foiEscrito=true`                                       |
| `Write`                    | valida tipo da expressao                              | marca `foiLido=true` para ids referenciados                   |
| `ChamadaProcedimento`      | valida nome, quantidade e tipos dos argumentos        | marca o procedimento como usado e ids dos argumentos como lidos |
| `Id` (em contexto de leitura) | valida declaracao                                  | marca `foiLido=true`                                          |
| `ExpBinaria`, `ExpUnaria`  | valida tipos dos operandos                            | avalia se a expressao e constante (suporta codigo morto)      |
| `Valor` (literais)         | retorna tipo imediato                                 | usado para decidir condicao constante                         |

### 3.4. Contrato de diagnostico

Ambos os visitors escrevem no mesmo formato, mas em listas separadas:

```java
record Diagnostico(
    Severidade severidade,   // ERRO_SEMANTICO | LINT
    String     codigo,       // ex: "SEM_ID_NAO_DECLARADO", "LINT_VAR_NAO_USADA"
    String     mensagem,
    String     contexto      // nome do proc, id, ou regiao
) {}
```

Essa separacao mantem a condicao do plano: erros semanticos e avisos de linter saem em formato uniforme, mas nunca misturados no mesmo stream.

## 4. Resumo Executivo

- a AST da Imperativa 2 e uma arvore de tres hierarquias — `Comando`, `Declaracao`, `Expressao` — com cerca de 25 classes concretas e duas listas auxiliares
- a tabela de simbolos e uma pilha de mapas `Id -> T`; para o linter, e preciso um `Simbolo` enriquecido com `kind`, `foiLido` e `foiEscrito`
- visitors ainda nao existem na base; a proposta e adicionar dois visitors externos (`SemanticVisitor` e `LinterVisitor`) que rodam apos o parser e compartilham um `AmbienteAnalise` com pilha de escopos
- a Opcao A (dispatch por `instanceof`) permite introduzir a analise estatica sem modificar os arquivos existentes da AST, o que mantem intacta a execucao atual da linguagem
