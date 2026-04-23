# Fase 1 - Levantamento e Entendimento da Base

## Resultado da Analise

Este documento registra o levantamento completo da base de codigo da Linguagem Imperativa 2, identificando a estrutura atual, o que funciona, e as lacunas tecnicas que precisam ser resolvidas antes de implementar o analisador semantico e o linter.

## 1. Mapa dos Arquivos Principais

### Ponto de Entrada

- `PLP_2026/Imperativa2/src/li2/plp/imperative2/parser/Imperative2.jj` — parser JavaCC, constroi a AST e executa
- `PLP_2026/Imperativa2/src/li2/plp/imperative2/Programa.java` — raiz da AST, contem o comando principal

### Comandos (AST)

- `imperative1/command/Comando.java` — interface base com `executar()` e `checaTipo()`
- `imperative1/command/IfThenElse.java` — condicional
- `imperative1/command/While.java` — laco
- `imperative1/command/Atribuicao.java` — atribuicao `id := expressao`
- `imperative1/command/Write.java` — saida `write(expressao)`
- `imperative1/command/Read.java` — entrada `read(id)`
- `imperative1/command/Skip.java` — comando vazio
- `imperative1/command/SequenciaComando.java` — sequencia `comando ; comando`
- `imperative1/command/ComandoDeclaracao.java` — bloco `{ declaracao ; comando }`
- `imperative2/command/ChamadaProcedimento.java` — chamada `call id(args)`
- `imperative2/command/ListaExpressao.java` — lista de expressoes para argumentos

### Declaracoes

- `imperative1/declaration/Declaracao.java` — classe abstrata base com `elabora()` e `checaTipo()`
- `imperative1/declaration/DeclaracaoVariavel.java` — `var id = expressao`
- `imperative1/declaration/DeclaracaoComposta.java` — `declaracao , declaracao`
- `imperative2/declaration/DeclaracaoProcedimento.java` — `proc id(...) { comando }`
- `imperative2/declaration/DefProcedimento.java` — definicao interna do procedimento (parametros + corpo)
- `imperative2/declaration/DeclaracaoParametro.java` — parametro individual
- `imperative2/declaration/ListaDeclaracaoParametro.java` — lista de parametros

### Expressoes

- `expressions2/expression/Expressao.java` — interface base com `avaliar()`, `checaTipo()`, `getTipo()`
- `expressions2/expression/ExpBinaria.java` — base abstrata para operacoes binarias
  - Subclasses: `ExpSoma`, `ExpSub`, `ExpAnd`, `ExpOr`, `ExpEquals`, `ExpConcat`
- `expressions2/expression/ExpUnaria.java` — base abstrata para operacoes unarias
  - Subclasses: `ExpMenos`, `ExpNot`, `ExpLength`
- `expressions2/expression/Id.java` — identificador
- `expressions2/expression/Valor.java` — interface para valores
- `expressions2/expression/ValorConcreto.java` — base generica para valores
  - Subclasses: `ValorInteiro`, `ValorBooleano`, `ValorString`

### Memoria e Escopo

- `expressions2/memory/Contexto.java` — pilha de `HashMap<Id, T>`, base do controle de escopo
- `expressions2/memory/ContextoExecucao.java` — contexto para valores (`Contexto<Valor>`)
- `expressions2/memory/ContextoCompilacao.java` — contexto para tipos (`Contexto<Tipo>`)
- `imperative1/memory/ContextoExecucaoImperativa.java` — adiciona I/O (ListaValor)
- `imperative2/memory/ContextoExecucaoImperativa2.java` — adiciona suporte a procedimentos

### Tipos

- `expressions1/util/Tipo.java` — interface de tipo
- `expressions1/util/TipoPrimitivo.java` — enum com INTEIRO, BOOLEANO, STRING
- `imperative2/util/TipoProcedimento.java` — tipo de procedimento (lista de tipos dos parametros)

## 2. Diagrama do Pipeline Atual

```text
Codigo-fonte
  -> Imp2Parser.Input()          [JavaCC le tokens, constroi objetos AST]
  -> Programa(comando)           [raiz da arvore]
  -> programa.checaTipo(ctx)     [cada no chama checaTipo nos filhos]
  -> programa.executar(ctx)      [cada no chama executar nos filhos]
  -> saida (ListaValor)
```

Nao existe visitor separado. Toda logica de tipo e execucao esta embutida nos proprios nos da AST via `checaTipo()`, `executar()` e `avaliar()`.

## 3. Pipeline Pretendido (com analise estatica)

```text
Codigo-fonte
  -> Imp2Parser.Input()          [parser constroi AST]
  -> Programa(comando)           [raiz da arvore]
  -> Visitor Semantico           [percorre AST, preenche tabela de simbolos, valida tipos]
  -> Visitor Linter              [percorre AST validada, emite avisos de qualidade]
  -> Relatorio de erros e avisos
```

## 4. O Que Ja Funciona Bem (Nao Precisa Mudar)

### Expressoes — getters completos

- `ExpBinaria`: `getEsq()`, `getDir()`, `getOperador()` — publicos
- `ExpUnaria`: `getExp()`, `getOperador()` — publicos
- `Id`: `getIdName()` — publico
- `ValorConcreto`: `valor()` — publico
- `ValorInteiro`, `ValorBooleano`, `ValorString` — herdam de ValorConcreto

### Declaracoes parcialmente acessiveis

- `DeclaracaoVariavel`: `getId()`, `getExpressao()` — publicos
- `DefProcedimento`: `getComando()`, `getParametrosFormais()` — publicos

### Controle de escopo

- `Contexto<T>` com `Stack<HashMap<Id, T>>` — solido
- `incrementa()` empilha novo escopo, `restaura()` desempilha
- `map(Id, T)` insere no topo, `get(Id)` busca do topo para baixo
- Escopos separados para variaveis e procedimentos

### Infraestrutura de tipos

- `TipoPrimitivo` (INTEIRO, BOOLEANO, STRING) com metodos `eInteiro()`, `eBooleano()`, `eString()`, `eIgual()`
- `TipoProcedimento` — tipo de procedimento baseado na lista de tipos dos parametros

## 5. Lacunas Tecnicas — O Que Precisa Ser Implementado

### 5.1. Getters publicos ausentes

Os visitors precisam percorrer a AST lendo os campos internos de cada no. As seguintes classes tem campos `private` sem getter publico. Sem esses getters, nenhum visitor consegue funcionar.

| Classe | Campos que precisam de getter publico |
|---|---|
| `Programa` | `comando` |
| `IfThenElse` | `expressao`, `comandoThen`, `comandoElse` |
| `While` | `expressao`, `comando` |
| `Atribuicao` | `id`, `expressao` |
| `Write` | `expressao` |
| `Read` | `id` |
| `SequenciaComando` | `comando1`, `comando2` |
| `ComandoDeclaracao` | `declaracao`, `comando` |
| `ChamadaProcedimento` | `nomeProcedimento`, `parametrosReais` |
| `DeclaracaoComposta` | `declaracao1`, `declaracao2` |
| `DeclaracaoProcedimento` | `id`, `defProcedimento` (getters existem mas sao `private`, precisam virar `public`) |

Getters sugeridos por classe:

```java
// Programa.java
public Comando getComando()

// IfThenElse.java
public Expressao getExpressao()
public Comando getComandoThen()
public Comando getComandoElse()

// While.java
public Expressao getExpressao()
public Comando getComando()

// Atribuicao.java
public Id getId()
public Expressao getExpressao()

// Write.java
public Expressao getExpressao()

// Read.java
public Id getId()

// SequenciaComando.java
public Comando getComando1()
public Comando getComando2()

// ComandoDeclaracao.java
public Declaracao getDeclaracao()
public Comando getComando()

// ChamadaProcedimento.java
public Id getNomeProcedimento()
public ListaExpressao getParametrosReais()

// DeclaracaoComposta.java
public Declaracao getDeclaracao1()
public Declaracao getDeclaracao2()

// DeclaracaoProcedimento.java
// mudar de private para public:
public Id getId()
public DefProcedimento getDefProcedimento()
```

### 5.2. Estrategia de Visitor

O projeto nao possui interface Visitor nem metodo `accept()` nas classes da AST. Existem duas abordagens possiveis:

**Opcao A — Visitor classico com accept() (mais trabalho, mais elegante)**

Requer adicionar um metodo `accept(Visitor v)` em cada classe da AST e criar a interface Visitor com um metodo `visit()` para cada tipo de no. Vantagem: extensibilidade limpa. Desvantagem: toca em todas as classes existentes.

**Opcao B — Visitor por instanceof (mais simples, recomendado)**

Criar visitors que recebem o no e fazem despacho por `instanceof`. Nao requer modificar as classes existentes alem dos getters. Vantagem: menos invasivo, mais rapido de implementar. Desvantagem: menos elegante, mas perfeitamente funcional para o escopo do projeto.

Recomendacao: **Opcao B**. O projeto tem um numero limitado de tipos de no, o que torna o `instanceof` manejavel. Isso evita tocar na estrutura existente e permite focar no que importa: a analise semantica e o linter.

Exemplo de estrutura do visitor por instanceof:

```java
public class VisitorSemantico {

    public void visitar(Comando cmd) {
        if (cmd instanceof IfThenElse) {
            visitarIfThenElse((IfThenElse) cmd);
        } else if (cmd instanceof While) {
            visitarWhile((While) cmd);
        } else if (cmd instanceof Atribuicao) {
            visitarAtribuicao((Atribuicao) cmd);
        }
        // ... demais tipos
    }

    private void visitarIfThenElse(IfThenElse cmd) {
        visitar(cmd.getExpressao());   // precisa do getter
        visitar(cmd.getComandoThen()); // precisa do getter
        visitar(cmd.getComandoElse()); // precisa do getter
    }
}
```

### 5.3. Precedencia de operadores no parser (Fase 2)

O parser atual (`Imperative2.jj`) trata todos os operadores binarios no mesmo nivel de precedencia. Problemas observados:

- `1 + 2 == 3` e parseado como `1 + (2 == 3)` em vez de `(1 + 2) == 3`
- `true or false and true` nao respeita que `and` tem maior precedencia que `or`
- Operadores unarios (`-`, `not`, `length`) chamam `PExpressao()` ao inves de `PExpPrimaria()`, fazendo com que `-x + 1` seja parseado como `-(x + 1)`

A fatoracao em 3 niveis descrita no plano de implementacao ainda nao foi aplicada:

```text
ExpBinaria  ::= ExpBinaria2 [ "==" ExpBinaria2 ]
ExpBinaria2 ::= ExpBinaria3 { ("+" | "-" | "or" | "++") ExpBinaria3 }
ExpBinaria3 ::= ExpUnaria { "and" ExpUnaria }
```

Isso sera tratado na Fase 2.

## 6. Validacoes Semanticas Existentes vs. Necessarias

### Ja existem (embutidas em checaTipo)

- Verificacao de tipo booleano em condicoes de `if` e `while`
- Verificacao de compatibilidade de tipo em atribuicoes
- Verificacao de tipo em operacoes binarias e unarias
- Verificacao de declaracao de variavel antes do uso (via `ambiente.get()`)
- Verificacao de tipos dos parametros em chamadas de procedimento

### Precisam ser criadas (no visitor semantico)

- Tabela de simbolos propria, separada do contexto de execucao
- Registro de variavel como "declarada", "escrita" e "lida" (para o linter)
- Registro de procedimentos com seus parametros formais
- Validacao de escopo completa com pilha gerenciada pelo visitor
- Separacao clara entre erro semantico e aviso de linter
- Acumulo de multiplos erros/avisos em uma unica passagem

## 7. Onde Encaixar o Visitor Semantico e o Linter

### Visitor Semantico

- Recebe a AST construida pelo parser (objeto `Programa`)
- Percorre toda a arvore, visitando comandos, declaracoes e expressoes
- Preenche uma tabela de simbolos propria
- Valida declaracoes, escopo, tipos e chamadas
- Retorna lista de erros semanticos

### Visitor Linter

- Recebe a AST ja validada semanticamente
- Usa a tabela de simbolos preenchida pelo visitor semantico
- Aplica regras de qualidade: variaveis nao utilizadas, codigo morto, complexidade
- Retorna lista de avisos de linter

### Ponto de integracao no pipeline

Dentro do `main()` de `Imp2Parser`, apos o parse e antes (ou no lugar) da execucao:

```java
Programa programa = parser.Input();

// Fase semantica (novo)
VisitorSemantico semantico = new VisitorSemantico();
List<ErroSemantico> erros = semantico.analisar(programa);

if (erros.isEmpty()) {
    // Fase linter (novo)
    VisitorLinter linter = new VisitorLinter(semantico.getTabelaSimbolos());
    List<AvisoLinter> avisos = linter.analisar(programa);

    // Exibir avisos
    for (AvisoLinter aviso : avisos) {
        System.out.println(aviso);
    }
}

// Exibir erros
for (ErroSemantico erro : erros) {
    System.out.println(erro);
}
```

## 8. Criterio de Saida da Fase 1

- [x] Mapa dos arquivos principais documentado
- [x] Diagrama do pipeline atual e pretendido
- [x] Lista completa de lacunas tecnicas identificada
- [x] Getters necessarios listados por classe
- [x] Estrategia de visitor definida (instanceof)
- [x] Ponto de integracao no pipeline identificado
- [x] Validacoes existentes vs. necessarias catalogadas

## 9. Proximos Passos (Fase 2)

1. Adicionar os getters publicos listados na secao 5.1
2. Tornar publicos os getters privados de `DeclaracaoProcedimento`
3. Corrigir a precedencia de operadores no parser (fatoracao em 3 niveis)
4. Corrigir operadores unarios para nao serem gulosos
5. Testar parser com expressoes de precedencia mista
