# Projeto Analisador Estático e Linter para Linguagem Imperativa 2

## Universidade Federal de Pernambuco

**Centro de Informática** <br>
**Disciplina:** IN1007-2026.1 - **Paradigmas de Linguagens de Programação**

## Equipe
* **Romulo Vital Domingos** - rvd@cin.ufpe.br
* **Nickolas Vitor Gomes da Silva** - nvgs@cin.ufpe.br


Este repositório organiza o projeto da disciplina de Paradigmas de Linguagens de Programação com o código-fonte do fork concentrado em `PLP_2026/` e a documentação geral na raiz. O foco deste trabalho é a construção de um analisador estático e linter para a Linguagem Imperativa 2.

## Introdução

Este trabalho foi desenvolvido no contexto da disciplina de Paradigmas de Linguagens de Programação e propõe uma aplicação prática sobre a Linguagem Imperativa 2. A ideia central é investigar o programa não apenas pelo que ele executa, mas principalmente pela sua estrutura, pelas relações entre comandos e pelo comportamento que pode ser inferido estaticamente.

Ao direcionar o projeto para análise estática, o estudo passa a integrar conceitos de compiladores, gramáticas formais, árvores sintáticas abstratas, escopo e verificação semântica. Assim, o repositório deixa de ser apenas uma base de execução da linguagem e passa a servir como base para inspeção, validação e identificação de más práticas de programação.

## Objetivos

### Objetivo Geral

Desenvolver um analisador estático com funcionalidades de linter para programas escritos na Linguagem Imperativa 2, utilizando a estrutura sintática e semântica do projeto como suporte para detectar problemas de qualidade e organização lógica do código.

### Objetivos Específicos

- Adaptar a gramática da Linguagem Imperativa 2 para uma forma compatível com o parser utilizado no projeto.
- Utilizar a AST como estrutura principal para as etapas de análise.
- Apoiar a análise semântica com controle de escopo e organização da tabela de símbolos.
- Implementar verificações voltadas para variáveis não utilizadas, código morto e complexidade de procedimentos.
- Relacionar a implementação prática aos conceitos de sintaxe concreta, sintaxe abstrata e análise estática estudados na disciplina.

## Estrutura do Repositório

- `PLP_2026/`: código-fonte do fork, módulos Maven, parser, AppUI e documentação técnica original.
- `README.md`: visão geral do projeto, escopo acadêmico e resumo da proposta.
- `apresentacoes/`: espaço reservado para slides e material de defesa.

## Resumo do Projeto

#### 1. Escopo e Objetivos

O projeto consiste na criação de uma ferramenta de análise de código que identifica problemas semânticos e de estilo em programas escritos na Linguagem Imperativa 2. O foco principal não é a tradução ou execução, mas a inspeção profunda da estrutura lógica do programa.

Funcionalidades previstas do linter:

- Detecção de variáveis não utilizadas: identificar variáveis que ocupam escopo, mas nunca são lidas.
- Identificação de código morto: detectar blocos de comandos que, por lógica de controle, nunca serão executados.
- Análise de complexidade de procedimentos: medir a densidade de decisões como `if` e `while` dentro de um `proc`.

#### 2. Arquitetura do Pipeline

A construção segue o modelo clássico de compiladores, mas com o backend voltado para análise em vez de geração de código:

- Análise léxica (`JavaCC`): converte o código-fonte em um fluxo de tokens.
- Análise sintática (`JavaCC/JJTree`): valida a gramática e constrói a AST.
- Análise semântica (`Visitor 1`): percorre a AST para preencher a tabela de símbolos e validar tipos.
- Análise do linter (`Visitor 2`): percorre a AST validada em busca de padrões de má prática.

#### 3. Adaptação da BNF

Para que a gramática da Imperativa 2 funcione corretamente em um gerador como o JavaCC, foram aplicados ajustes técnicos importantes:

- Fatoração de expressões: a regra de expressões binárias precisa ser reorganizada para remover recursão à esquerda indireta e preservar precedência entre operadores.
- Simplificação da AST: símbolos da sintaxe concreta, como parênteses, chaves e `:=`, são descartados na árvore abstrata, preservando apenas a estrutura lógica relevante.

#### 4. O Coração da Construção: Padrão Visitor

O padrão Visitor separa a estrutura da árvore da lógica de análise. A AST permanece focada em representar o programa, enquanto o comportamento do linter fica isolado em visitantes especializados.

Exemplos de análise:

- Ao encontrar um `proc`, o visitor inicia um contador de complexidade. Cada visita a nós como `IfThenElse` e `While` incrementa esse valor. Se o total ultrapassar um limite definido, o linter emite um alerta de alta complexidade.
- Ao analisar `IfThenElse`, o visitor pode verificar se a condição é um valor booleano constante. Se a condição for sempre `false`, o bloco `then` é marcado como inalcançável. Se for sempre `true`, o bloco `else` passa a ser o trecho morto.

#### 5. Justificativa Teórica

Este projeto é um estudo de caso adequado para a disciplina porque evidencia conceitos centrais de linguagens de programação:

- Separação de preocupações: diferença entre sintaxe concreta e sintaxe abstrata.
- Gestão de escopo: tratamento de variáveis globais, locais e parâmetros de procedimentos.
- Análise de fluxo: inspeção do comportamento do programa sem necessidade de execução.

## BNF de Referência

Nesta seção, a BNF original é apresentada como referência conceitual da linguagem. Em seguida, são destacadas as adaptações feitas na gramática implementada no parser, especialmente na parte de expressões. Os trechos em negrito correspondem à forma fatorada usada para tornar a gramática compatível com análise descendente e explicitar a precedência entre operadores.

## BNF Original

```text
Programa ::= Comando

Comando ::= Atribuicao
          | ComandoDeclaracao
          | While
          | IfThenElse
          | IO
          | Comando ";" Comando
          | Skip
          | ChamadaProcedimento

Skip ::= ε

Atribuicao ::= Id ":=" Expressao

Expressao ::= Valor
            | ExpUnaria
            | ExpBinaria
            | Id

Valor ::= ValorConcreto

ValorConcreto ::= ValorInteiro
                | ValorBooleano
                | ValorString

ExpUnaria ::= "-" Expressao
            | "not" Expressao
            | "length" Expressao

ExpBinaria ::= Expressao "+" Expressao
             | Expressao "-" Expressao
             | Expressao "and" Expressao
             | Expressao "or" Expressao
             | Expressao "==" Expressao
             | Expressao "++" Expressao

ComandoDeclaracao ::= "{" Declaracao ";" Comando "}"

Declaracao ::= DeclaracaoVariavel
             | DeclaracaoProcedimento
             | DeclaracaoComposta

DeclaracaoVariavel ::= "var" Id "=" Expressao

DeclaracaoComposta ::= Declaracao "," Declaracao

DeclaracaoProcedimento ::= "proc" Id "(" [ ListaDeclaracaoParametro ] ")" "{" Comando "}"

ListaDeclaracaoParametro ::= Tipo Id
                           | Tipo Id "," ListaDeclaracaoParametro

Tipo ::= "string" | "int" | "boolean"

While ::= "while" Expressao "do" Comando

IfThenElse ::= "if" Expressao "then" Comando "else" Comando

IO ::= "write" "(" Expressao ")"
     | "read" "(" Id ")"

ChamadaProcedimento ::= "call" Id "(" [ ListaExpressao ] ")"

ListaExpressao ::= Expressao
                 | Expressao "," ListaExpressao
```
### Prova Teórica da Recursão à Esquerda Indireta

A BNF original apresenta recursão à esquerda indireta na definição de expressões binárias. Isso pode ser observado pelas regras:

```text
Expressao ::= Valor | ExpUnaria | ExpBinaria | Id
ExpBinaria ::= Expressao "+" Expressao | ...
```

A derivação abaixo evidencia o problema:

```text
ExpBinaria
=> Expressao "+" Expressao
=> ExpBinaria "+" Expressao
```

Essa cadeia mostra que `ExpBinaria` pode voltar a si mesma pelo lado esquerdo da derivação, mas de forma indireta, passando antes por `Expressao`. Em parsers descendentes, como os normalmente construídos com JavaCC, essa forma de definição é inadequada porque dificulta a escolha de produção e compromete a análise sintática previsível.

### Regras Adaptadas para o Parser

As regras abaixo correspondem às alterações centrais feitas sobre a BNF original. Elas representam a forma usada na implementação atual do parser:

**ExpBinaria ::= ExpBinaria2 [ "==" ExpBinaria2 ]**

**ExpBinaria2 ::= ExpBinaria3 { ("+" | "-" | "or" | "++") ExpBinaria3 }**

**ExpBinaria3 ::= ExpUnaria { "and" ExpUnaria }**

Essas alterações reorganizam a gramática em níveis, permitindo tratar precedência e associatividade de forma mais adequada ao processo de análise sintática.

Os principais pontos técnicos dessa adaptação são:

- Na BNF original, `ExpBinaria` é definida a partir de `Expressao`, e `Expressao` também pode derivar para `ExpBinaria`. Isso introduz recursão à esquerda indireta.
- A gramática original reúne vários operadores binários em uma única regra, sem separar claramente níveis de precedência. Isso torna mais difícil controlar a interpretação de expressões compostas.
- A fatoração em `ExpBinaria`, `ExpBinaria2` e `ExpBinaria3` divide o processamento em camadas, refletindo melhor a precedência dos operadores.
- A reorganização também favorece a construção da AST, porque cada nível sintático passa a representar um grupo mais específico de operações.

### Validação da Gramática Adaptada

A validação prática foi feita executando a versão atual do parser com expressões e programas válidos da Linguagem Imperativa 2. Como a implementação já utiliza a gramática fatorada, os testes não reproduzem a BNF original literalmente; eles verificam que a adaptação adotada funciona corretamente.

Exemplos de entradas usadas na validação:

- `write(1 + 2 + 3)`, com resultado `6`.
- `write(true or false and true)`, com resultado `true`.
- Blocos com declaração e chamada de procedimento, como o caso em que uma variável `a` é atualizada por `incA(z)` e o programa imprime `9`.

Esses testes reforçam que a implementação atual analisa e executa corretamente expressões compostas, chamadas de procedimento e comandos da linguagem.



## Navegação da Implementação

Esta seção funciona como um mapa entre os elementos da BNF e os arquivos principais do projeto, facilitando a navegação entre a descrição formal da linguagem e sua implementação.

### Mapeamento BNF -> Implementação

- `Programa` -> [Programa.java](./PLP_2026/Imperativa2/src/li2/plp/imperative2/Programa.java)
- `Comando` -> comandos base em [imperative1/command](./PLP_2026/Imperativa2/src/li2/plp/imperative1/command)
- `Atribuicao` -> [Atribuicao.java](./PLP_2026/Imperativa2/src/li2/plp/imperative1/command/Atribuicao.java)
- `ComandoDeclaracao` -> [ComandoDeclaracao.java](./PLP_2026/Imperativa2/src/li2/plp/imperative1/command/ComandoDeclaracao.java)
- `While` -> [While.java](./PLP_2026/Imperativa2/src/li2/plp/imperative1/command/While.java)
- `IfThenElse` -> [IfThenElse.java](./PLP_2026/Imperativa2/src/li2/plp/imperative1/command/IfThenElse.java)
- `IO` -> [Write.java](./PLP_2026/Imperativa2/src/li2/plp/imperative1/command/Write.java) e [Read.java](./PLP_2026/Imperativa2/src/li2/plp/imperative1/command/Read.java)
- `Skip` -> [Skip.java](./PLP_2026/Imperativa2/src/li2/plp/imperative1/command/Skip.java)
- `ChamadaProcedimento` -> [ChamadaProcedimento.java](./PLP_2026/Imperativa2/src/li2/plp/imperative2/command/ChamadaProcedimento.java)
- `ListaExpressao` -> [ListaExpressao.java](./PLP_2026/Imperativa2/src/li2/plp/imperative2/command/ListaExpressao.java)
- `DeclaracaoVariavel` -> [DeclaracaoVariavel.java](./PLP_2026/Imperativa2/src/li2/plp/imperative1/declaration/DeclaracaoVariavel.java)
- `DeclaracaoComposta` -> [DeclaracaoComposta.java](./PLP_2026/Imperativa2/src/li2/plp/imperative1/declaration/DeclaracaoComposta.java)
- `DeclaracaoProcedimento` -> [DeclaracaoProcedimento.java](./PLP_2026/Imperativa2/src/li2/plp/imperative2/declaration/DeclaracaoProcedimento.java)
- `Expressao` -> [Expressao.java](./PLP_2026/Imperativa2/src/li2/plp/expressions2/expression/Expressao.java)
- `Id` -> [Id.java](./PLP_2026/Imperativa2/src/li2/plp/expressions2/expression/Id.java)
- `Valor` -> [Valor.java](./PLP_2026/Imperativa2/src/li2/plp/expressions2/expression/Valor.java)
- `ValorInteiro` -> [ValorInteiro.java](./PLP_2026/Imperativa2/src/li2/plp/expressions2/expression/ValorInteiro.java)
- `ValorBooleano` -> [ValorBooleano.java](./PLP_2026/Imperativa2/src/li2/plp/expressions2/expression/ValorBooleano.java)
- `ValorString` -> [ValorString.java](./PLP_2026/Imperativa2/src/li2/plp/expressions2/expression/ValorString.java)
- `ValorConcreto` -> [ValorConcreto.java](./PLP_2026/Imperativa2/src/li2/plp/expressions2/expression/ValorConcreto.java)
- `Tipo` -> [Tipo.java](./PLP_2026/Imperativa2/src/li2/plp/expressions1/util/Tipo.java)

## Classes Auxiliares

- [AmbienteExecucaoImperativa2](./PLP_2026/Imperativa2/src/li2/plp/imperative2/memory/AmbienteExecucaoImperativa2.java)
- [ContextoExecucaoImperativa2](./PLP_2026/Imperativa2/src/li2/plp/imperative2/memory/ContextoExecucaoImperativa2.java)
- [ListaValor](./PLP_2026/Imperativa2/src/li2/plp/imperative1/memory/ListaValor.java)
- [DefProcedimento](./PLP_2026/Imperativa2/src/li2/plp/imperative2/declaration/DefProcedimento.java)
- [ProcedimentoJaDeclaradoException](./PLP_2026/Imperativa2/src/li2/plp/imperative2/memory/ProcedimentoJaDeclaradoException.java)
- [ProcedimentoNaoDeclaradoException](./PLP_2026/Imperativa2/src/li2/plp/imperative2/memory/ProcedimentoNaoDeclaradoException.java)

## Parser

- [Imperative2](./PLP_2026/Imperativa2/src/li2/plp/imperative2/parser/Imperative2.jj)

