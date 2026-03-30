# Projeto Analisador Estático e Linter para Linguagem Imperativa 2

## Universidade Federal de Pernambuco

**Centro de informática**
**Disciplina:** IN1007-2026.1 - **Paradigmas de Linguagens de programação**

## Equipe
* **Romulo Vital Domingos** - rvd@cin.ufpe.br
*  


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

- Fatoração de expressões: a regra de expressões binárias precisa ser reorganizada para remover recursão à esquerda e preservar precedência.
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

Nesta seção, os trechos em negrito indicam as adaptações realizadas sobre a BNF original para torná-la compatível com o parser. O principal ajuste ocorreu na fatoração das expressões, necessária para evitar recursão à esquerda e explicitar a precedência entre operadores.

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

### Regras Adaptadas para o Parser

As regras abaixo correspondem às alterações centrais feitas sobre a BNF original:

**ExpBinaria ::= ExpBinaria2 [ "==" ExpBinaria2 ]**

**ExpBinaria2 ::= ExpBinaria3 { ("+" | "-" | "or" | "++") ExpBinaria3 }**

**ExpBinaria3 ::= ExpUnaria { "and" ExpUnaria }**

Essas alterações reorganizam a gramática em níveis, permitindo tratar precedência e associatividade de forma mais adequada ao processo de análise sintática.

## Classes Auxiliares

- `AmbienteExecucaoImperativa2`
- `ContextoExecucaoImperativa2`
- `ListaValor`
- `DefProcedimento`
- `ProcedimentoJaDeclaradoException`
- `ProcedimentoNaoDeclaradoException`

## Parser

- `Imperative2`

