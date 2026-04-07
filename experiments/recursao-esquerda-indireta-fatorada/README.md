# Experimento Fatorado de Expressoes

Este modulo e a contraparte corrigida do experimento de recursao a esquerda indireta.

Aqui a gramatica foi reorganizada em niveis para funcionar bem com parser descendente:

- `Expression`
- `EqualityExpression`
- `AdditiveExpression`
- `UnaryExpression`
- `PrimaryExpression`

## Como executar

```bash
cd /Users/romulodomingos/Documents/plp/IN1007-PLP-2026.1/experiments/recursao-esquerda-indireta-fatorada
mvn generate-sources compile
printf '1 + 2 - 3\n' | mvn -q exec:java
printf 'not (1 + 2)\n' | mvn -q exec:java
```

## Resultado esperado

O parser deve ser gerado e compilado normalmente. As entradas acima devem terminar com:

```text
Parse concluido sem erro.
```

## Comparacao conceitual

- modulo `recursao-esquerda-indireta`: falha porque a recursao aparece antes da fatoracao
- modulo `recursao-esquerda-indireta-fatorada`: funciona porque a expressao foi separada em niveis previsiveis
