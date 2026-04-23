# Plano de Implementacao do Projeto

## Analisador Estatico e Linter para Linguagem Imperativa 2

Este documento organiza a execucao do projeto em quatro frentes complementares:

- fases de desenvolvimento
- backlog priorizado
- cronograma sugerido
- roteiro tecnico de implementacao

O objetivo e sair de uma base que ja executa programas da Linguagem Imperativa 2 para uma ferramenta que tambem analisa a AST, valida aspectos semanticos e gera avisos de linter.

## 1. Visao Geral da Arquitetura

Pipeline pretendido:

```text
Codigo-fonte
-> Analise lexica e sintatica (JavaCC/JJTree)
-> AST
-> Analise semantica
-> Analise de linter
-> Relatorio de erros e avisos
```

Separacao de responsabilidades:

- Parser: reconhece a linguagem e construi a AST.
- Analise semantica: valida declaracoes, escopo, tipos e chamadas.
- Linter: identifica problemas de qualidade, manutencao e organizacao logica.

Resultados esperados:

- erros sintaticos
- erros semanticos
- avisos de linter

## 1.1. Escopo Recomendado para Entrega

Para este projeto, o caminho tecnicamente mais forte e academicamente mais defensavel e priorizar a implementacao do analisador estatico com linter sobre a base atual da linguagem, em vez de investir muito tempo recriando a gramatica original com erro.

### Obrigatorio

- manter a gramatica atual funcional
- utilizar a AST existente como base da analise
- implementar a analise semantica com escopo e tabela de simbolos
- implementar as regras principais do linter
- produzir mensagens claras de erro semantico e aviso de linter
- criar exemplos e testes que comprovem o funcionamento da ferramenta

Justificativa:

- esse e o nucleo do projeto proposto
- esse escopo entrega funcionalidade real sobre a Linguagem Imperativa 2
- esse recorte evidencia AST, visitor, escopo, semantica e analise estatica

### Bom ter

- manter um experimento isolado da gramatica original com recursao a esquerda indireta
- usar esse experimento como demonstracao em apresentacao ou documentacao
- comparar a BNF original com a versao fatorada usada no parser
- mostrar por que a adaptacao da gramatica foi necessaria

Justificativa:

- isso fortalece a fundamentacao teorica
- ajuda a defender as decisoes de parser
- agrega valor academico, mas nao deve consumir o tempo principal da implementacao

### Dispensavel

- transformar o experimento da gramatica original no foco central do trabalho
- gastar muito tempo em interface visual para o experimento
- tentar reconstruir toda a linguagem primeiro na versao com erro
- adiar o linter para aprofundar apenas a parte de parser

Justificativa:

- isso desloca o foco do objetivo principal do projeto
- aumenta o risco de atraso sem melhorar muito a entrega final
- o valor maior do trabalho esta na analise semantica e nas regras do linter

### Recomendacao Final

Estrutura de prioridade sugerida:

1. parser atual estavel
2. analise semantica
3. linter
4. testes
5. documentacao
6. experimento da gramatica original apenas como apoio teorico

Em outras palavras, o experimento com erro e util, mas deve ser tratado como material complementar. O centro da entrega deve ser a ferramenta que analisa programas validos da Imperativa 2 e produz diagnosticos relevantes.

## 2. Fases do Projeto

### Fase 1. Levantamento e entendimento da base

Objetivo:
Entender como o fork atual organiza parser, AST, execucao, memoria e semantica.

Atividades:

- revisar a estrutura do modulo `PLP_2026/Imperativa2`
- mapear classes da AST e comandos principais
- localizar o parser em `Imperative2.jj`
- identificar como o projeto representa escopos, variaveis e procedimentos
- registrar quais validacoes semanticas ja existem e quais precisam ser criadas
- definir onde o linter sera encaixado no fluxo

Entregaveis:

- mapa dos arquivos principais
- diagrama simples do pipeline atual
- lista de lacunas tecnicas a implementar

Criterio de saida:

- a equipe sabe exatamente onde editar parser, semantica e linter

### Fase 2. Consolidacao da gramatica e da AST

Objetivo:
Corrigir as lacunas tecnicas identificadas na Fase 1 para que a gramatica adaptada trate precedencia corretamente e a AST seja util para analise estatica.

Atividades:

- revisar a BNF original e a versao fatorada
- confirmar eliminacao da recursao a esquerda indireta
- aplicar em `Imperative2.jj` a fatoracao em 3 niveis das expressoes binarias:
  - `ExpBinaria  ::= ExpBinaria2 [ "==" ExpBinaria2 ]`
  - `ExpBinaria2 ::= ExpBinaria3 { ("+" | "-" | "or" | "++") ExpBinaria3 }`
  - `ExpBinaria3 ::= ExpUnaria { "and" ExpUnaria }`
- corrigir operadores unarios gulosos (`-`, `not`, `length`) para chamarem `PExpPrimaria()` em vez de `PExpressao()`, evitando que `-x + 1` seja lido como `-(x + 1)`
- validar precedencia com entradas como `1 + 2 == 3`, `true or false and true`, `-x + 1`
- adicionar getters publicos ausentes nas classes listadas na Fase 1 secao 5.1 (`Programa`, `IfThenElse`, `While`, `Atribuicao`, `Write`, `Read`, `SequenciaComando`, `ComandoDeclaracao`, `ChamadaProcedimento`, `DeclaracaoComposta`)
- tornar publicos os getters atualmente privados de `DeclaracaoProcedimento` (`getId`, `getDefProcedimento`)
- verificar se a AST descarta simbolos da sintaxe concreta sem perda semantica
- testar blocos com declaracoes e chamadas de procedimento

Entregaveis:

- parser estavel com precedencia e associatividade corretas
- AST com getters publicos suficientes para os visitors percorrerem a arvore
- conjunto inicial de entradas validas e invalidas cobrindo expressoes de precedencia mista

Criterio de saida:

- o parser aceita os programas-alvo com a arvore correta para cada nivel de precedencia
- toda classe da AST relevante ao linter expoe os filhos necessarios via getter publico
- um visitor esqueleto consegue descer pela AST sem usar reflexao nem acessar campos privados

### Fase 3. Estruturacao da analise semantica

Objetivo:
Construir a base que dara suporte confiavel ao linter.

Atividades:

- definir ou revisar tabela de simbolos
- tratar escopo global, escopo local e parametros
- validar declaracao e uso de identificadores
- validar compatibilidade de tipos em expressoes e atribuicoes
- validar chamadas de procedimento e quantidade de argumentos
- separar claramente erro semantico de aviso de linter

Entregaveis:

- visitor semantico funcional
- estrutura de ambiente/tabela de simbolos documentada
- conjunto de erros semanticos padronizados

Criterio de saida:

- o programa pode ser validado semanticamente antes da etapa de linter

### Fase 4. Implementacao das regras do linter

Objetivo:
Adicionar analises estaticas de qualidade sobre a AST validada.

Atividades:

- implementar regra de variavel nao utilizada
- implementar regra de codigo morto com base em condicoes constantes
- implementar metrica de complexidade por procedimento
- padronizar o formato das mensagens do linter
- permitir acumulo de multiplos avisos em uma unica execucao

Entregaveis:

- visitor de linter
- estrutura de avisos
- exemplos demonstrando cada regra

Criterio de saida:

- o linter gera avisos corretos para os casos definidos no projeto

### Fase 5. Integracao e testes

Objetivo:
Comprovar que parser, semantica e linter funcionam de forma integrada.

Atividades:

- criar casos de teste unitarios e funcionais
- organizar entradas por categoria
- validar a ordem do pipeline
- revisar falsos positivos e falsos negativos
- testar programas com multiplos avisos

Entregaveis:

- suite de testes
- pasta com programas de exemplo
- tabela de entradas e saidas esperadas

Criterio de saida:

- a ferramenta apresenta comportamento consistente e reproduzivel

### Fase 6. Documentacao e apresentacao

Objetivo:
Transformar a implementacao em um trabalho academico bem defendido.

Atividades:

- consolidar explicacao teorica da gramatica
- explicar AST, visitor, escopo e linter
- documentar arquitetura e fluxo de dados
- preparar slides e demonstracao
- registrar limitacoes e trabalhos futuros

Entregaveis:

- README consolidado
- slides
- demonstracao preparada

Criterio de saida:

- a equipe consegue justificar as decisoes tecnicas e demonstrar a ferramenta

## 3. Backlog Priorizado

### Epic A. Base do parser e AST

Prioridade: alta

- A1. Revisar `Imperative2.jj` e documentar as producoes principais.
- A2. Validar a fatoracao das expressoes binaria e unaria.
- A3. Confirmar os nos da AST necessarios para analise semantica e linter.
- A4. Criar exemplos minimos para cada construcao da linguagem.

Definicao de pronto:

- parser aceitando exemplos validos e rejeitando exemplos invalidos basicos

### Epic B. Analise semantica

Prioridade: alta

- B1. Definir a estrutura de simbolo para variavel, parametro e procedimento.
- B2. Implementar controle de escopo para blocos e procedimentos.
- B3. Validar declaracao antes do uso.
- B4. Validar tipos em atribuicoes.
- B5. Validar tipos em expressoes unarias e binarias.
- B6. Validar chamadas de procedimento e argumentos.

Definicao de pronto:

- visitor semantico retornando erros confiaveis para os casos de uso previstos

### Epic C. Linter de variaveis nao utilizadas

Prioridade: alta

- C1. Registrar toda variavel declarada no escopo.
- C2. Marcar leituras de variavel durante a travessia da AST.
- C3. Distinguir declaracao sem leitura de mera atribuicao.
- C4. Emitir aviso ao encerrar o escopo quando a variavel nunca for lida.

Definicao de pronto:

- avisos corretos para variaveis declaradas e nao usadas sem falsos positivos obvios

### Epic D. Linter de codigo morto

Prioridade: media

- D1. Identificar expressoes booleanas constantes.
- D2. Detectar `if true` e `if false`.
- D3. Detectar `while false`.
- D4. Emitir aviso no ramo ou corpo inalcançavel.
- D5. Avaliar se ha espaco para inferencias adicionais sem aumentar muito a complexidade.

Definicao de pronto:

- avisos emitidos para trechos certamente inalcançaveis

### Epic E. Linter de complexidade de procedimentos

Prioridade: media

- E1. Definir a metrica de complexidade usada no projeto.
- E2. Iniciar contagem ao entrar em cada `proc`.
- E3. Incrementar em `if`, `while` e outros desvios escolhidos pela equipe.
- E4. Configurar limiar inicial.
- E5. Emitir aviso quando o procedimento ultrapassar o limite.

Definicao de pronto:

- cada procedimento recebe uma avaliacao simples e compreensivel

### Epic F. Infraestrutura de diagnosticos

Prioridade: media

- F1. Criar classe para erro ou aviso com tipo, mensagem e contexto.
- F2. Padronizar formato textual das mensagens.
- F3. Decidir se mensagens exibem nome do no, nome do procedimento ou identificador.
- F4. Preparar impressao ordenada dos diagnosticos.

Definicao de pronto:

- erros e avisos saem em formato uniforme

### Epic G. Testes e demonstracao

Prioridade: alta

- G1. Criar casos de teste para parser.
- G2. Criar casos de teste para semantica.
- G3. Criar casos de teste para cada regra do linter.
- G4. Criar programas compostos para demonstracao final.
- G5. Registrar saidas esperadas.

Definicao de pronto:

- o projeto tem evidencia objetiva de corretude e valor pratico

## 4. Quadro de Tarefas

Sugestao de organizacao em Kanban:

### Coluna: A Fazer

- Revisar parser `Imperative2.jj`
- Mapear AST usada em `Imperativa2`
- Definir estrutura da tabela de simbolos
- Definir interface do visitor semantico
- Definir interface do visitor de linter
- Definir classe de diagnostico
- Escrever testes de parser
- Escrever testes de semantica
- Escrever testes de linter

### Coluna: Em Andamento

- Validacao da gramatica fatorada
- Controle de escopo para blocos e procedimentos
- Regra de variavel nao utilizada

### Coluna: Em Revisao

- Deteccao de codigo morto em `if` e `while`
- Metrica de complexidade por procedimento
- Padronizacao das mensagens

### Coluna: Concluido

- Mapeamento BNF para implementacao
- Definicao do escopo academico do projeto
- Documento de objetivos e justificativa teorica

## 5. Cronograma Sugerido

### Semana 1. Leitura tecnica e mapeamento

- estudar `README.md` e `Imperative2.jj`
- mapear AST, comandos, declaracoes e memoria
- listar lacunas de parser, semantica e linter

Meta da semana:

- entendimento completo da base atual

### Semana 2. Parser e AST

- revisar regras de expressoes
- testar precedencia e associatividade
- confirmar nos relevantes da AST

Meta da semana:

- parser confiavel e AST utilizavel

### Semana 3. Analise semantica

- implementar tabela de simbolos
- validar declaracoes, escopo e tipos
- validar chamadas de procedimento

Meta da semana:

- visitor semantico operacional

### Semana 4. Linter 1

- implementar variaveis nao utilizadas
- criar testes dedicados

Meta da semana:

- primeira regra completa do linter

### Semana 5. Linter 2

- implementar codigo morto
- avaliar condicoes constantes
- criar testes dedicados

Meta da semana:

- segunda regra completa do linter

### Semana 6. Linter 3

- implementar complexidade de procedimentos
- ajustar limiares e mensagens

Meta da semana:

- terceira regra completa do linter

### Semana 7. Integracao e refinamento

- integrar parser, semantica e linter
- revisar diagnosticos
- reduzir falsos positivos

Meta da semana:

- ferramenta consistente ponta a ponta

### Semana 8. Documentacao e apresentacao

- consolidar README
- preparar slides
- selecionar programas de demonstracao

Meta da semana:

- material final pronto para entrega e defesa

## 6. Roteiro Tecnico de Implementacao

### Etapa 1. Confirmar o ponto de entrada

Arquivos para inspecao inicial:

- `PLP_2026/Imperativa2/src/li2/plp/imperative2/parser/Imperative2.jj`
- `PLP_2026/Imperativa2/src/li2/plp/imperative2/Programa.java`
- `PLP_2026/Imperativa2/src/li2/plp/imperative1/command`
- `PLP_2026/Imperativa2/src/li2/plp/imperative2/declaration`
- `PLP_2026/Imperativa2/src/li2/plp/imperative2/memory`

Perguntas tecnicas:

- onde a AST e criada
- como a execucao atual consome essa AST
- onde encaixar o visitor semantico
- onde encaixar o visitor de linter

### Etapa 2. Projetar a analise semantica

Estruturas recomendadas:

- classe de simbolo
- tabela de simbolos por escopo
- pilha de escopos
- resultado de diagnostico

Responsabilidades do visitor semantico:

- visitar declaracoes
- abrir e fechar escopos
- registrar variaveis e parametros
- registrar procedimentos
- validar tipos
- validar chamadas

Saida:

- lista de erros semanticos
- possivel AST anotada ou contexto derivado para o linter

### Etapa 3. Projetar a infraestrutura do linter

Estruturas recomendadas:

- classe `LintWarning`
- enum ou constante para tipos de aviso
- contexto do procedimento atual
- contexto do escopo atual

Responsabilidades do visitor de linter:

- visitar a AST apos analise semantica
- acumular avisos
- isolar cada regra em metodos claros

Saida:

- lista de avisos ordenados e padronizados

### Etapa 4. Implementar regra de variavel nao utilizada

Estrategia:

- ao visitar declaracoes de variavel, registrar nome e escopo
- ao visitar `Id` em contexto de leitura, marcar uso
- ao fechar o escopo, verificar quais simbolos nunca foram lidos

Cuidados:

- nao confundir escrita com leitura
- tratar parametros como simbolos analisaveis
- evitar avisar sobre simbolos inexistentes quando ja houver erro semantico

### Etapa 5. Implementar regra de codigo morto

Estrategia:

- avaliar se uma expressao booleana e constante
- se a condicao de `if` for constante, marcar um dos ramos como morto
- se a condicao de `while` for `false`, marcar o corpo como inalcançavel

Cuidados:

- limitar a inferencia a casos seguramente estaticos
- evitar interpretacoes agressivas que gerem falso positivo

### Etapa 6. Implementar regra de complexidade

Estrategia:

- ao entrar em um `proc`, iniciar contador
- incrementar ao visitar `if` e `while`
- ao sair do procedimento, comparar com o limiar configurado

Cuidados:

- documentar a regra de contagem adotada
- manter o criterio simples para facilitar a defesa academica

### Etapa 7. Integrar diagnosticos a interface de uso

Possiveis saidas:

- impressao em console
- relatorio simples por programa analisado
- separacao entre erros e avisos

Formato sugerido:

```text
ERRO SEMANTICO: identificador 'x' nao declarado.
LINT: variavel 'temp' declarada, mas nunca utilizada.
LINT: ramo else inalcançavel porque a condicao e sempre true.
LINT: procedimento 'calc' possui complexidade 6, acima do limite 4.
```

## 7. Casos de Teste Minimos Recomendados

### Parser

- `write(1 + 2 + 3)`
- `write(true or false and true)`
- `call p(1, 2)`
- `{ var x = 1; write(x) }`

### Semantica

- uso de variavel nao declarada
- atribuicao com tipo incompativel
- chamada de procedimento com quantidade errada de argumentos

### Linter

- variavel declarada e nunca lida
- `if true then C1 else C2`
- `if false then C1 else C2`
- `while false do C`
- procedimento com muitos `if` e `while`

## 8. Divisao Sugerida da Equipe

### Frente 1. Parser e semantica

Responsabilidades:

- revisar gramatica
- consolidar AST
- implementar tabela de simbolos
- implementar visitor semantico

### Frente 2. Linter e testes

Responsabilidades:

- implementar warnings
- criar visitor de linter
- construir testes por regra
- preparar demonstracoes

Ponto de integracao:

- AST validada semanticamente

## 9. Riscos Tecnicos e Mitigacoes

- Risco: misturar validacao semantica com linter.
- Mitigacao: manter visitors separados e contratos claros.

- Risco: gramatica aceitar casos ambiguos de expressao.
- Mitigacao: testar precedencia e associatividade com entradas pequenas.

- Risco: muitos falsos positivos em codigo morto.
- Mitigacao: limitar a deteccao a expressoes constantes evidentes.

- Risco: dificuldade para rastrear uso de variavel em escopos aninhados.
- Mitigacao: usar pilha de escopos com fechamento explicito.

## 10. Checklist Final de Entrega

- parser funcionando com a gramatica adaptada
- AST adequada para visitors
- analise semantica separada do linter
- regra de variavel nao utilizada implementada
- regra de codigo morto implementada
- regra de complexidade implementada
- mensagens padronizadas
- casos de teste executados
- README atualizado
- slides e demonstracao preparados

## 11. Proximo Passo Recomendado

Ordem objetiva para comecar a implementacao:

1. confirmar o fluxo do parser e da AST em `Imperativa2`
2. projetar a tabela de simbolos e o visitor semantico
3. implementar a infraestrutura de diagnosticos
4. entregar primeiro a regra de variavel nao utilizada
5. seguir para codigo morto
6. fechar com complexidade de procedimentos

Essa ordem reduz retrabalho porque primeiro estabiliza a base semantica e depois adiciona as regras de linter sobre uma AST ja validada.
