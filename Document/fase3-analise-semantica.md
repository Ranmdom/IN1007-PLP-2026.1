# Fase 3 - Estruturacao da Analise Semantica

## Resultado da Execucao

Este documento registra a implementacao da Fase 3: tabela de simbolos propria, visitor semantico por `instanceof`, classe de erro semantico padronizada e integracao no pipeline. O resultado e um analisador que percorre a AST construida pela Fase 2, acumula multiplos erros semanticos em uma unica execucao e expoe (atraves da tabela de simbolos) os contadores de leitura/escrita que a Fase 4 (linter) vai consumir.

A Fase 3 mantem como invariante:

- nao depende dos `checaTipo()` antigos para detectar erros (faz a propria verificacao de tipos)
- nao executa o programa (apenas analisa)
- nao emite avisos de qualidade (isso e Fase 4); aqui so sai erro semantico

## 1. Arquitetura

Pipeline atualizado:

```text
Codigo-fonte
  -> Imp2Parser.Input()           [parser JavaCC, Fase 2]
  -> Programa(comando)            [raiz da AST]
  -> VisitorSemantico.analisar()  [NOVO - Fase 3]
       -> percorre AST por instanceof
       -> preenche TabelaSimbolos (Simbolo, Escopo)
       -> acumula erros em List<ErroSemantico>
  -> se nao houver erros: programa.checaTipo() + programa.executar() (legado)
  -> se houver erros: imprime e aborta
```

Pacote novo: `li2.plp.imperative2.semantica` em [PLP_2026/Imperativa2/src/li2/plp/imperative2/semantica/](../PLP_2026/Imperativa2/src/li2/plp/imperative2/semantica/).

## 2. Tabela de Simbolos

A tabela e separada do `Contexto<Tipo>` usado pelos `checaTipo()` antigos. As razoes:

- precisa guardar mais que tipo (kind, contadores de leitura/escrita, parametros formais de procedimento)
- nao deve ser descartada quando o escopo fecha — o linter da Fase 4 precisa olhar simbolos que ja sairam de cena

### 2.1. Classes

| Classe | Responsabilidade |
|---|---|
| [`SimboloKind`](../PLP_2026/Imperativa2/src/li2/plp/imperative2/semantica/SimboloKind.java) | enum: `VARIAVEL`, `PARAMETRO`, `PROCEDIMENTO` |
| [`Simbolo`](../PLP_2026/Imperativa2/src/li2/plp/imperative2/semantica/Simbolo.java) | nome, `Tipo`, kind, nivel do escopo, contadores `lido`/`escrito`, parametros formais (opcional) |
| [`Escopo`](../PLP_2026/Imperativa2/src/li2/plp/imperative2/semantica/Escopo.java) | nivel, ref ao pai, `LinkedHashMap<String, Simbolo>` (preserva ordem de declaracao) |
| [`TabelaSimbolos`](../PLP_2026/Imperativa2/src/li2/plp/imperative2/semantica/TabelaSimbolos.java) | escopo atual + lista de escopos fechados; `abrirEscopo`, `fecharEscopo`, `declarar`, `buscar`, `buscarLocal`, `getTodosEscopos` |

### 2.2. Modelo de escopo

- A tabela comeca com um **escopo global** (nivel 0).
- Cada `ComandoDeclaracao` (`{ declaracao ; comando }`) abre um novo escopo, processa a declaracao, visita o comando e fecha o escopo.
- Cada `DeclaracaoProcedimento` abre **outro** escopo para o corpo do procedimento, onde os parametros formais sao declarados antes de visitar o comando do corpo.
- `fecharEscopo()` nao destroi o escopo: move ele para a lista `escoposFechados`. Isso preserva a informacao para a Fase 4 (que vai ler `lido`/`escrito` por simbolo).
- `buscar(nome)` faz lookup encadeado: escopo atual primeiro, depois pais ate a raiz. Isso permite que um identificador interno **sombreie** (`shadow`) um externo com o mesmo nome (validado pelo teste 12).

### 2.3. Por que `LinkedHashMap`

Os simbolos sao guardados na ordem em que foram declarados dentro de cada escopo. Quando a Fase 4 for emitir avisos de "variavel nao utilizada", a saida ja sai determinista, na mesma ordem do codigo-fonte, sem precisar ordenar depois.

## 3. Erro Semantico

Classe [`ErroSemantico`](../PLP_2026/Imperativa2/src/li2/plp/imperative2/semantica/ErroSemantico.java): codigo + mensagem + contexto opcional.

Codigos definidos:

| Codigo | Quando ocorre |
|---|---|
| `IDENTIFICADOR_NAO_DECLARADO` | uso de `Id` (em expressao, atribuicao, read ou call) que nao existe na tabela |
| `IDENTIFICADOR_JA_DECLARADO` | redeclaracao no mesmo escopo (variavel, procedimento ou parametro duplicado) |
| `IDENTIFICADOR_NAO_E_VARIAVEL` | atribuicao a procedimento, `read` em procedimento, uso de procedimento como valor |
| `IDENTIFICADOR_NAO_E_PROCEDIMENTO` | `call` em variavel ou parametro |
| `TIPO_INCOMPATIVEL_ATRIBUICAO` | `id := exp` com tipos diferentes |
| `TIPO_CONDICAO_NAO_BOOLEANO` | condicao de `if`/`while` que nao e booleana |
| `TIPO_OPERADOR_INCOMPATIVEL` | operandos com tipos errados em operador binario/unario |
| `ARIDADE_PROCEDIMENTO` | numero de argumentos diferente do numero de parametros formais |
| `TIPO_ARGUMENTO_PROCEDIMENTO` | argumento com tipo diferente do parametro formal correspondente |

Formato textual:

```text
ERRO SEMANTICO [CODIGO]: mensagem
```

Esse formato deixa claro, no terminal, qual a categoria do erro e prepara o terreno para o linter da Fase 4 (que vai usar o prefixo `LINT [...]` para nao confundir com erro).

## 4. VisitorSemantico

Arquivo: [VisitorSemantico.java](../PLP_2026/Imperativa2/src/li2/plp/imperative2/semantica/VisitorSemantico.java).

### 4.1. Estrategia

Despacho por `instanceof` (escolhido na Fase 1, secao 5.2). Tres pontos de despacho:

- `visitarComando(Comando)` — `Skip`, `SequenciaComando`, `ComandoDeclaracao`, `IfThenElse`, `While`, `Atribuicao`, `Write`, `Read`, `ChamadaProcedimento`
- `visitarDeclaracao(Declaracao)` — `DeclaracaoComposta`, `DeclaracaoVariavel`, `DeclaracaoProcedimento`
- `tipoDe(Expressao)` — `ValorInteiro/Booleano/String`, `Id`, todas as subclasses de `ExpBinaria` (`ExpSoma`, `ExpSub`, `ExpAnd`, `ExpOr`, `ExpEquals`, `ExpConcat`), todas de `ExpUnaria` (`ExpMenos`, `ExpNot`, `ExpLength`)

### 4.2. Acumulo de erros

O visitor nao lanca excecao no primeiro erro. Ele:

1. registra o erro em `List<ErroSemantico> erros`
2. continua a analise
3. usa `null` como marcador de "tipo desconhecido" — quando uma sub-expressao retorna `null`, o chamador evita emitir o proximo erro daquela cadeia, para nao gerar cascata

O teste 13 (`13_acumulo_multiplos_erros.imp`) verifica esse comportamento: emite 4 erros distintos em uma unica passagem.

### 4.3. Regras de tipo implementadas

| Construcao | Regra |
|---|---|
| `Id` (uso) | deve existir e nao ser `PROCEDIMENTO`; incrementa `lido` |
| `id := exp` | id deve existir e nao ser `PROCEDIMENTO`; tipo de exp deve casar com tipo declarado; incrementa `escrito` |
| `read(id)` | id deve existir e nao ser `PROCEDIMENTO`; incrementa `escrito` |
| `write(exp)` | visita exp (qualquer tipo aceito) |
| `if cond then C1 else C2` | tipo de `cond` deve ser `BOOLEANO` |
| `while cond do C` | tipo de `cond` deve ser `BOOLEANO` |
| `call id(args)` | id deve existir e ser `PROCEDIMENTO`; aridade(args) == aridade(formais); cada arg[i] com tipo igual ao parametro[i] |
| `e1 + e2` / `e1 - e2` | ambos `INTEIRO` -> `INTEIRO` |
| `e1 and e2` / `e1 or e2` | ambos `BOOLEANO` -> `BOOLEANO` |
| `e1 ++ e2` | ambos `STRING` -> `STRING` |
| `e1 == e2` | mesmo tipo dos dois lados -> `BOOLEANO` |
| `- e` | `INTEIRO` -> `INTEIRO` |
| `not e` | `BOOLEANO` -> `BOOLEANO` |
| `length e` | `STRING` -> `INTEIRO` |

Para `==`, a regra "mesmo tipo dos dois lados" cobre comparacao entre dois inteiros, dois booleanos ou duas strings, e rejeita `1 == true` etc.

### 4.4. Coexistencia com `checaTipo()` legado

A Fase 3 nao remove os `checaTipo()` espalhados pelas classes da AST. A ordem no `main` e:

1. parse
2. `VisitorSemantico.analisar(programa)` — se houver erros, imprime e aborta antes de qualquer execucao
3. se passou pelo visitor, ainda chama `programa.checaTipo(...)` legado por seguranca
4. se passou tambem no `checaTipo`, executa

Essa redundancia e proposital: o visitor e a fonte primaria de erros semanticos (com mensagens estruturadas), o `checaTipo` permanece como uma rede de seguranca que ja existia. Quando a Fase 4 (linter) entrar em cena, ela vai rodar entre os passos 2 e 3, consumindo a tabela de simbolos preenchida pelo visitor.

## 5. Integracao no Pipeline

Modificacao em [Imperative2.jj](../PLP_2026/Imperativa2/src/li2/plp/imperative2/parser/Imperative2.jj):

- import do pacote novo: `import li2.plp.imperative2.semantica.*;`
- bloco novo no `main()`, executado entre o parse e o `checaTipo` legado:

```java
VisitorSemantico semantico = new VisitorSemantico();
List<ErroSemantico> errosSemanticos = semantico.analisar(programa);
if (!errosSemanticos.isEmpty()) {
    System.out.println("==== ERROS SEMANTICOS ====");
    for (ErroSemantico erro : errosSemanticos) {
        System.out.println(erro);
    }
    System.out.println("Total: " + errosSemanticos.size()
            + " erro(s) semantico(s). Execucao abortada.");
    return;
}
System.out.println("Analise semantica: ok.");
```

Resultado: todo programa que passa pelo parser agora tambem passa pelo visitor antes de executar. O linter da Fase 4 vai entrar exatamente no mesmo lugar, depois do visitor.

## 6. Validacao

### 6.1. Build

```sh
cd PLP_2026/Imperativa2
mvn -q clean compile
```

Saida: `Parser generated successfully.` e nenhum erro de compilacao.

### 6.2. Suite de testes

Pasta: [PLP_2026/Imperativa2/testes-fase3/](../PLP_2026/Imperativa2/testes-fase3/). Sao 15 programas — 4 validos (esperam `Analise semantica: ok.` + execucao) e 11 invalidos (esperam codigo de erro especifico).

| Arquivo | Programa | Esperado | Resultado |
|---|---|---|---|
| `01_valido_completo.imp` | bloco com var, proc, 2 chamadas, write | ok + saida `2` | ok + `2` |
| `02_id_nao_declarado.imp` | `write(x + 1)` | `IDENTIFICADOR_NAO_DECLARADO` | ✓ |
| `03_atribuicao_tipo_errado.imp` | `var x = 1; x := true` | `TIPO_INCOMPATIVEL_ATRIBUICAO` | ✓ |
| `04_condicao_nao_booleana.imp` | `if x + 1 then ... else ...` | `TIPO_CONDICAO_NAO_BOOLEANO` (if) | ✓ |
| `05_while_condicao_int.imp` | `while x do ...` (x int) | `TIPO_CONDICAO_NAO_BOOLEANO` (while) | ✓ |
| `06_chamada_aridade_errada.imp` | proc espera 2, recebe 1 | `ARIDADE_PROCEDIMENTO` | ✓ |
| `07_chamada_tipo_argumento.imp` | proc int,int recebe int,bool | `TIPO_ARGUMENTO_PROCEDIMENTO` | ✓ |
| `08_proc_nao_declarado.imp` | `call inexistente()` | `IDENTIFICADOR_NAO_DECLARADO` | ✓ |
| `09_call_em_variavel.imp` | `call x()` onde x e int | `IDENTIFICADOR_NAO_E_PROCEDIMENTO` | ✓ |
| `10_uso_proc_como_valor.imp` | `write(p + 1)` onde p e proc | `IDENTIFICADOR_NAO_E_VARIAVEL` | ✓ |
| `11_redeclaracao_mesmo_escopo.imp` | `var x = 1, var x = 2` | `IDENTIFICADOR_JA_DECLARADO` | ✓ |
| `12_escopo_aninhado_valido.imp` | `x:int` externo, `x:bool` interno sombreia | ok + saida `10` | ok + `10` |
| `13_acumulo_multiplos_erros.imp` | 1 programa com 4 erros distintos | 4 erros em uma execucao | ✓ (4 erros) |
| `14_concat_tipo_errado.imp` | `"abc" ++ 1` | `TIPO_OPERADOR_INCOMPATIVEL` (++) | ✓ |
| `15_parametro_acessivel_no_corpo.imp` | proc usa seu parametro x | ok + saida `10` | ok + `10` |

Saida abreviada do teste 13 (acumulo de erros):

```text
==== ERROS SEMANTICOS ====
ERRO SEMANTICO [TIPO_INCOMPATIVEL_ATRIBUICAO]: atribuicao incompativel: 'x' tem tipo INTEIRO mas a expressao tem tipo BOOLEANO
ERRO SEMANTICO [IDENTIFICADOR_NAO_DECLARADO]: identificador 'y' nao declarado (atribuicao)
ERRO SEMANTICO [TIPO_CONDICAO_NAO_BOOLEANO]: condicao do 'if' deve ser booleana, mas tem tipo INTEIRO
ERRO SEMANTICO [IDENTIFICADOR_NAO_DECLARADO]: identificador 'y' nao declarado
Total: 4 erro(s) semantico(s). Execucao abortada.
```

Saida do teste 01 (valido):

```text
Imperativa 2 PLP Parser Version 0.0.1:  Imperativa2 program parsed successfully.
Analise semantica: ok.
2
```

### 6.3. Programa pre-existente

O arquivo [PLP_2026/Testes/TesteImperativa2.txt](../PLP_2026/Testes/TesteImperativa2.txt), que ja existia no repositorio, tambem passa na analise semantica (a execucao so falha porque ele tem um `read(a)` sem entrada fornecida no commandline — limitacao ambiental, nao do visitor).

### 6.4. Como reproduzir

Mesmo padrao da Fase 2: o `pom.xml` chama o parser com o arquivo `input` fixo. Para rodar um teste:

```sh
cd PLP_2026/Imperativa2
cp testes-fase3/03_atribuicao_tipo_errado.imp input
mvn -q exec:java 2>&1 | grep -v -E "Return:|Call:|Consumed token:|Visited token:" | tail -10
```

## 7. Criterio de Saida da Fase 3

- [x] Tabela de simbolos propria, separada do `Contexto<Tipo>` legado, documentada (secao 2).
- [x] Escopo global, escopo local (`ComandoDeclaracao`) e escopo de procedimento (parametros + corpo) tratados corretamente — validado pelos testes 01, 12, 15.
- [x] Validacao de declaracao antes do uso — testes 02 e 08.
- [x] Validacao de compatibilidade de tipos em expressoes e atribuicoes — testes 03, 04, 05, 14.
- [x] Validacao de chamada de procedimento (aridade e tipos) — testes 06, 07.
- [x] Distincao clara entre erro semantico (Fase 3) e aviso de linter (Fase 4): erros saem com prefixo `ERRO SEMANTICO [CODIGO]:`, linter usara prefixo proprio.
- [x] Acumulo de multiplos erros em uma unica execucao — teste 13 (4 erros simultaneos).
- [x] Ponto de integracao no pipeline — feito em `Imperative2.jj`.
- [x] Visitor por `instanceof` operacional (conforme escolha da Fase 1).
- [x] Conjunto de erros padronizados — 9 codigos em [ErroSemantico.Codigo](../PLP_2026/Imperativa2/src/li2/plp/imperative2/semantica/ErroSemantico.java).

## 8. O Que Falta Para a Fase 4

A Fase 3 deixou pronto:

- a tabela de simbolos com contadores `lido`/`escrito` ja sendo incrementados pelo visitor
- a lista de escopos fechados acessivel via `tabela.getTodosEscopos()`
- a integracao no pipeline (basta inserir o visitor de linter entre o semantico e o `checaTipo` legado)

Para a Fase 4 entrar:

- criar `VisitorLinter` em `li2.plp.imperative2.linter` (ou similar) que recebe a `TabelaSimbolos` ja preenchida pelo `VisitorSemantico`
- regra **variavel nao utilizada**: percorrer `tabela.getTodosEscopos()`, para cada `Simbolo` com `kind == VARIAVEL` (e talvez `PARAMETRO`) cujo `getLido() == 0`, emitir aviso. Os contadores ja existem; nada precisa ser adicionado a Fase 3.
- regra **codigo morto**: precisa de um avaliador parcial de expressoes booleanas constantes (`true`, `false`, `not true`, `true and false`, etc.). O visitor de linter percorre `IfThenElse`/`While` e marca o ramo morto. Isso e novo e nao depende da tabela.
- regra **complexidade por procedimento**: visitor mantem um contador local ao entrar em `DeclaracaoProcedimento`, incrementa em cada `IfThenElse` e `While` dentro do corpo, compara com um limite (configuravel) ao sair.
- criar `AvisoLinter` analogo a `ErroSemantico`, com prefixo `LINT [CODIGO]:`.
- modificar `main()` em `Imperative2.jj` para rodar o `VisitorLinter` entre o semantico e o `checaTipo` legado, somente se o semantico passou.

Nada disso foi feito ainda. Os artefatos da Fase 3 sao suficientes para que a Fase 4 nao precise tocar em nada da AST, do parser ou do pipeline existente.
