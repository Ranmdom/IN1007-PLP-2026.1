# Fase 2 - Consolidacao da Gramatica e da AST

## Resultado da Execucao

Este documento registra o que foi efetivamente feito na Fase 2: a refatoracao da gramatica de expressoes em `Imperative2.jj` para corrigir precedencia e associatividade, e a abertura da AST atraves de getters publicos para permitir que visitors (Fase 3 e seguintes) percorram a arvore sem reflexao e sem acessar campos privados.

A Fase 1 ja estava completa e documentada em `fase1-levantamento-e-mapeamento.md`. As lacunas tecnicas que ela apontou foram a entrada da Fase 2.

## 1. Estado da Fase 1 (Pre-condicao)

Verificado antes de comecar. Todos os criterios de saida da Fase 1 estavam marcados como concluidos no documento:

- Mapa dos arquivos principais documentado
- Diagrama do pipeline atual e pretendido
- Lista completa de lacunas tecnicas identificada
- Getters necessarios listados por classe (secao 5.1 do doc da Fase 1)
- Estrategia de visitor definida (instanceof, sem `accept()`)
- Ponto de integracao no pipeline identificado
- Validacoes existentes vs. necessarias catalogadas

Nada da Fase 1 foi alterado.

## 2. Mudancas na Gramatica

Arquivo: [PLP_2026/Imperativa2/src/li2/plp/imperative2/parser/Imperative2.jj](../PLP_2026/Imperativa2/src/li2/plp/imperative2/parser/Imperative2.jj)

### 2.1. Problema anterior

Na versao antiga, todos os operadores binarios estavam no mesmo nivel e cada producao binaria era `PExpPrimaria() OP PExpressao()` (recursao a direita), com `PExpressao()` escolhendo entre `PExpUnaria`, `PExpBinaria` e `PExpPrimaria` via `LOOKAHEAD` sintatico. Consequencias observadas em Fase 1:

- `1 + 2 == 3` virava `1 + (2 == 3)` (erro de tipo, soma `int` com `boolean`); um programa valido era rejeitado pelo parser.
- `false and true or true` virava `false and (true or true)` em vez de `(false and true) or true`, ignorando que `and` tem precedencia maior que `or` (resultado `false` em vez do correto `true`).
- `1 - 2 - 3` virava `1 - (2 - 3)` (right-assoc) em vez de `(1 - 2) - 3` (resultado `2` em vez do correto `-4`).
- Os unarios `-`, `not`, `length` chamavam `PExpressao()`, entao `-1 + 2` virava `-(1 + 2)` (resultado `-3` em vez do correto `1`).

Observacao: o efeito de precedencia `and`/`or` so aparece quando o `and` vem antes do `or` na fonte. Em `true or false and true` a versao antiga, por acaso, produzia a arvore correta — por isso esse caso nao serve como exemplo do defeito. O caso que expoe o problema e `false and true or true`. Todos os casos acima foram verificados executando a gramatica antiga (commit anterior a Fase 2) e a nova; ver `experiments/demo-fase2/`.

### 2.2. Nova gramatica de expressoes

Fatoracao em 3 niveis, conforme planejado:

```text
ExpBinaria  ::= ExpBinaria2 [ "==" ExpBinaria2 ]              // nao associativo
ExpBinaria2 ::= ExpBinaria3 { ("+" | "-" | "or" | "++") ExpBinaria3 }   // left-assoc
ExpBinaria3 ::= ExpUnaria  { "and" ExpUnaria }                // left-assoc
ExpUnaria   ::= "-" ExpPrimaria
              | "not" ExpPrimaria
              | "length" ExpPrimaria
              | ExpPrimaria
ExpPrimaria ::= Id | Valor | "(" Expressao ")"
```

Ordem de precedencia (do mais fraco para o mais forte): `==` < `+` `-` `or` `++` < `and` < unarios.

A associatividade dos niveis 2 e 3 e a esquerda, implementada por loop com acumulador em vez de recursao a direita:

```jj
Expressao PExpBinaria2() :
{ Expressao acc; Expressao dir; }
{
    acc = PExpBinaria3()
    (
          <PLUS>   dir = PExpBinaria3() { acc = new ExpSoma(acc, dir); }
        | <MINUS>  dir = PExpBinaria3() { acc = new ExpSub(acc, dir); }
        | <OR>     dir = PExpBinaria3() { acc = new ExpOr(acc, dir); }
        | <CONCAT> dir = PExpBinaria3() { acc = new ExpConcat(acc, dir); }
    )*
    { return acc; }
}
```

### 2.3. Correcao dos unarios "gulosos"

`PExpMenos`, `PExpNot` e `PExpLength` agora chamam `PExpPrimaria()`, nao `PExpressao()`. Com isso, `-x + 1` vira `(-x) + 1`, porque a soma e capturada no nivel 2 sobre o resultado do unario.

### 2.4. Recursao a esquerda

A versao original ja estava sem recursao a esquerda explicita (cada `PExp<Op>` comeca por `PExpPrimaria()`, nao por ela mesma). A nova versao mantem esse padrao: nenhuma das novas producoes (`PExpBinaria`, `PExpBinaria2`, `PExpBinaria3`, `PExpUnaria`) inicia chamando a si mesma. A recursao a esquerda indireta tambem nao reaparece, ja que `PExpressao()` agora delega de forma linear: `PExpressao -> PExpBinaria -> PExpBinaria2 -> PExpBinaria3 -> PExpUnaria -> PExpPrimaria`.

### 2.5. AST preservada sem perda semantica

A nova gramatica continua produzindo as mesmas classes da AST ja existentes (`ExpSoma`, `ExpSub`, `ExpOr`, `ExpConcat`, `ExpEquals`, `ExpAnd`, `ExpMenos`, `ExpNot`, `ExpLength`). Nenhuma classe nova de no foi introduzida. O parser apenas passou a montar arvores com a estrutura correta de precedencia e associatividade.

Pontuacao da sintaxe concreta (`(`, `)`, `==`, `+`, etc.) continua descartada pelo parser e nao aparece na AST, como ja era antes.

## 3. AST: Getters Publicos Adicionados

Para que o visitor por `instanceof` (estrategia escolhida na Fase 1) consiga descer pela AST, cada no precisa expor seus filhos. Foram adicionados os seguintes getters publicos:

| Classe | Arquivo | Getters adicionados |
|---|---|---|
| `Programa` | [Programa.java](../PLP_2026/Imperativa2/src/li2/plp/imperative2/Programa.java) | `getComando()` |
| `IfThenElse` | [IfThenElse.java](../PLP_2026/Imperativa2/src/li2/plp/imperative1/command/IfThenElse.java) | `getExpressao()`, `getComandoThen()`, `getComandoElse()` |
| `While` | [While.java](../PLP_2026/Imperativa2/src/li2/plp/imperative1/command/While.java) | `getExpressao()`, `getComando()` |
| `Atribuicao` | [Atribuicao.java](../PLP_2026/Imperativa2/src/li2/plp/imperative1/command/Atribuicao.java) | `getId()`, `getExpressao()` |
| `Write` | [Write.java](../PLP_2026/Imperativa2/src/li2/plp/imperative1/command/Write.java) | `getExpressao()` |
| `Read` | [Read.java](../PLP_2026/Imperativa2/src/li2/plp/imperative1/command/Read.java) | `getId()` |
| `SequenciaComando` | [SequenciaComando.java](../PLP_2026/Imperativa2/src/li2/plp/imperative1/command/SequenciaComando.java) | `getComando1()`, `getComando2()` |
| `ComandoDeclaracao` | [ComandoDeclaracao.java](../PLP_2026/Imperativa2/src/li2/plp/imperative1/command/ComandoDeclaracao.java) | `getDeclaracao()`, `getComando()` |
| `ChamadaProcedimento` | [ChamadaProcedimento.java](../PLP_2026/Imperativa2/src/li2/plp/imperative2/command/ChamadaProcedimento.java) | `getNomeProcedimento()`, `getParametrosReais()` |
| `DeclaracaoComposta` | [DeclaracaoComposta.java](../PLP_2026/Imperativa2/src/li2/plp/imperative1/declaration/DeclaracaoComposta.java) | `getDeclaracao1()`, `getDeclaracao2()` |
| `DeclaracaoProcedimento` | [DeclaracaoProcedimento.java](../PLP_2026/Imperativa2/src/li2/plp/imperative2/declaration/DeclaracaoProcedimento.java) | `getId()` e `getDefProcedimento()` ja existiam, mudaram de `private` para `public` |

Classes que ja tinham os getters publicos antes (e nao foram tocadas): `ExpBinaria`, `ExpUnaria`, `Id`, `ValorConcreto`, `DeclaracaoVariavel`, `DefProcedimento`.

Com isso, o esqueleto de visitor desenhado na Fase 1 (secao 5.2) consegue descer pela AST inteira sem reflexao e sem acessar campos privados.

## 4. Validacao

### 4.1. Compilacao

`mvn -q compile` na pasta `PLP_2026/Imperativa2` regenera o parser via `javacc-maven-plugin` e compila o codigo gerado mais o codigo Java existente. Saida:

```text
Parser generated successfully.
```

Nenhum erro de compilacao apos as mudancas na gramatica e os getters adicionados.

### 4.2. Programas de teste

Foram criados em [PLP_2026/Imperativa2/testes-fase2/](../PLP_2026/Imperativa2/testes-fase2/) sete programas curtos para validar a precedencia, a associatividade, os unarios e a estrutura de blocos/procedimentos. Resultados obtidos:

| Arquivo | Programa | Saida | Esperado | Comentario |
|---|---|---|---|---|
| `test_sum_left_assoc.imp` | `write(1 + 2 + 3)` | `6` | `6` | soma associativa, ok |
| `test_sub_left_assoc.imp` | `write(1 - 2 - 3)` | `-4` | `(1-2)-3 = -4` | confirma left-assoc; antes da Fase 2 retornava `2` |
| `test_unary_minus_sum.imp` | `write(-1 + 2)` | `1` | `(-1)+2 = 1` | confirma unario nao-guloso; antes retornava `-3` |
| `test_eq_after_sum.imp` | `write(1 + 2 == 3)` | `true` | `(1+2)==3 = true` | antes dava erro de tipo (`int + boolean`) |
| `test_or_and.imp` | `write(false and true or true)` | `true` | `(false and true) or true = true` | `and` liga mais forte que `or`; antes da Fase 2 retornava `false` |
| `test_bloco_var.imp` | `{ var x = 10 ; write(x + 1) }` | `11` | `11` | bloco com declaracao de variavel |
| `test_bloco_proc.imp` | `{ proc dobra(int x) { write(x + x) } ; call dobra(5) }` | `10` | `10` | bloco com declaracao e chamada de procedimento |

Os tres ultimos casos sao especialmente importantes porque so passam a typecheck/executar com a gramatica fatorada nova; com a versao antiga, `1 + 2 == 3` falhava em `checaTipo` e `-1 + 2` retornava o valor errado.

### 4.3. Como reproduzir

Da raiz `PLP_2026/Imperativa2`:

```sh
# copiar o programa de teste sobre o arquivo "input" hardcoded no pom.xml
cp testes-fase2/test_eq_after_sum.imp input

# rodar
mvn -q exec:java
```

Saida esperada:

```text
Imperativa 2 PLP Parser Version 0.0.1:  Reading from file input . . .
Imperativa 2 PLP Parser Version 0.0.1:  Imperativa2 program parsed successfully.
true
```

## 5. Criterio de Saida da Fase 2

- [x] Parser estavel com precedencia e associatividade corretas (validado com 7 programas).
- [x] AST com getters publicos suficientes para que um visitor por `instanceof` percorra todos os comandos, declaracoes e expressoes.
- [x] Conjunto inicial de entradas validas cobrindo expressoes de precedencia mista (`==` sobre soma, `or`/`and` misturados, unario seguido de binario).
- [x] Recursao a esquerda continua eliminada na nova gramatica.
- [x] Visitor esqueleto consegue descer pela AST sem reflexao e sem acessar campos privados (verificado por compilacao: nenhuma classe de AST relevante mantem campos opacos).

## 6. O Que Falta Para a Fase 3

A Fase 3 ("Estruturacao da analise semantica") nao foi iniciada. Para entrar nela, com base no plano e no que ja existe:

- definir a classe `Simbolo` (nome, tipo, kind = variavel/parametro/procedimento, escopo de origem, contadores `lido`/`escrito` para alimentar o linter na Fase 4)
- definir a `TabelaSimbolos` propria do visitor, independente do `Contexto<Tipo>` ja usado em `checaTipo`. A estrutura existente (pilha de `HashMap<Id, T>` em `Contexto`) e um bom modelo, mas a tabela do visitor precisa guardar mais que so o tipo.
- criar a interface `VisitorSemantico` (despacho por `instanceof`, conforme decidido na Fase 1) cobrindo: `Programa`, todos os `Comando` (incluindo `SequenciaComando`, `ComandoDeclaracao`, `IfThenElse`, `While`, `Atribuicao`, `Read`, `Write`, `Skip`, `ChamadaProcedimento`), todas as `Declaracao` (`DeclaracaoVariavel`, `DeclaracaoComposta`, `DeclaracaoProcedimento`) e todas as `Expressao` (binarias, unarias, `Id`, valores).
- abrir/fechar escopos em `ComandoDeclaracao` e em corpos de procedimento.
- registrar parametros formais como simbolos do escopo do procedimento.
- validar declaracao antes de uso, compatibilidade de tipos em atribuicao, tipo das condicoes de `if`/`while`, aridade e tipos das chamadas de procedimento. Hoje isso esta espalhado nos `checaTipo()` dos nos da AST; a Fase 3 vai centralizar no visitor para que a Fase 4 (linter) tenha um lugar unico para acompanhar uso de variaveis e detectar codigo morto.
- definir a classe `ErroSemantico` com tipo, mensagem e contexto (no, identificador, escopo), e acumular varios erros em uma unica passagem em vez de abortar no primeiro.
- separar contratualmente erro semantico de aviso de linter. O linter da Fase 4 so deve rodar se a Fase 3 nao tiver erros, ou pelo menos so emitir avisos sobre subarvores bem tipadas.
- ponto de integracao no `main` de `Imp2Parser`: rodar o visitor semantico entre `parser.Input()` e `programa.executar(...)`, conforme esbocado em `fase1-levantamento-e-mapeamento.md` secao 7.

A AST e o parser ja estao prontos para receber esse trabalho.

## 7. O Que Falta Para a Fase 4

A Fase 4 ("Implementacao das regras do linter") depende da Fase 3. Resumo do que ela vai precisar:

- visitor de linter separado, que reaproveita a tabela de simbolos preenchida pela Fase 3 (especialmente os contadores `lido`/`escrito` por simbolo).
- regra de **variavel nao utilizada**: ao fechar cada escopo, emitir aviso para cada simbolo com contador de leitura zerado.
- regra de **codigo morto**: avaliar se a expressao da condicao de um `if`/`while` e booleana constante (`true`/`false` literais, possivelmente expressoes constantes simples), e marcar como inalcancavel o ramo nao tomado.
- regra de **complexidade por procedimento**: contar `if`s e `while`s dentro de cada `proc` e comparar com um limite configuravel.
- classe `AvisoLinter` (analoga a `ErroSemantico`) e impressao ordenada.

Nada disso foi feito ainda. A AST e os getters publicos entregues nesta Fase 2 sao a base para que tanto a Fase 3 quanto a Fase 4 consigam ser implementadas como visitors sem tocar nas classes existentes.
