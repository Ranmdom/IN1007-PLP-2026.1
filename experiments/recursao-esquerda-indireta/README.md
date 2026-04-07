# Experimento de Recursao a Esquerda Indireta

Este experimento existe para reproduzir, de forma isolada, o problema conceitual descrito no projeto principal: uma gramatica de expressoes em que `Expression` pode derivar para `BinaryExpression`, e `BinaryExpression` volta a depender de `Expression` no lado esquerdo antes de consumir tokens suficientes.

## Objetivo

Demonstrar a diferenca entre:

- uma formulacao problematica para parser descendente
- a formulacao fatorada usada no projeto principal

## Estrutura

- `pom.xml`: modulo Maven isolado para gerar e executar o parser
- `src/main/javacc/IndirectLeftRecursionDemo.jj`: gramatica minima com recursao a esquerda indireta

## Como executar

No terminal:

```bash
cd /Users/romulodomingos/Documents/plp/IN1007-PLP-2026.1/experiments/recursao-esquerda-indireta
mvn generate-sources compile
printf '1 + 2\n' | mvn -q exec:java
```

## O que observar

Entradas como estas tendem a expor o problema:

```text
1 + 2
1 - 2
not 1
(1 + 2)
```

Em uma gramatica descendente nessa forma, o parser tenta expandir `Expression` por `BinaryExpression`, e `BinaryExpression` chama `Expression` novamente no lado esquerdo. Isso pode produzir recursao profunda antes de um consumo estavel de tokens.

Dependendo da versao do JavaCC e do ambiente:

- o gerador pode aceitar a gramatica e o erro aparecer em tempo de execucao
- o gerador pode emitir avisos
- o parse pode terminar em `StackOverflowError` ou falha equivalente

## Comparacao com o projeto principal

No projeto principal, o parser atual evita essa estrutura ao separar as expressoes em niveis e ao comecar a expressao binaria a partir de uma expressao primaria no lado esquerdo.

Referencias:

- `/Users/romulodomingos/Documents/plp/IN1007-PLP-2026.1/README.md`
- `/Users/romulodomingos/Documents/plp/IN1007-PLP-2026.1/PLP_2026/Imperativa2/src/li2/plp/imperative2/parser/Imperative2.jj`

## Proximos passos sugeridos

1. Rodar o experimento com entradas pequenas.
2. Observar o ponto em que a derivacao volta para `Expression`.
3. Criar uma segunda versao fatorada da gramatica dentro deste mesmo experimento.
4. Comparar comportamento, clareza da AST e previsibilidade do parse.
