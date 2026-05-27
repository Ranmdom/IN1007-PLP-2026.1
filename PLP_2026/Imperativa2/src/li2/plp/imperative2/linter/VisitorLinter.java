package li2.plp.imperative2.linter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import li2.plp.expressions2.expression.Expressao;
import li2.plp.imperative1.command.Comando;
import li2.plp.imperative1.command.ComandoDeclaracao;
import li2.plp.imperative1.command.IfThenElse;
import li2.plp.imperative1.command.SequenciaComando;
import li2.plp.imperative1.command.While;
import li2.plp.imperative1.declaration.Declaracao;
import li2.plp.imperative1.declaration.DeclaracaoComposta;
import li2.plp.imperative2.Programa;
import li2.plp.imperative2.declaration.DeclaracaoProcedimento;
import li2.plp.imperative2.semantica.Escopo;
import li2.plp.imperative2.semantica.Simbolo;
import li2.plp.imperative2.semantica.SimboloKind;
import li2.plp.imperative2.semantica.TabelaSimbolos;

public class VisitorLinter {

    private static final int LIMIAR_COMPLEXIDADE = 4;

    private final TabelaSimbolos tabela;
    private final AvaliadorConstante avaliador = new AvaliadorConstante();
    private final List<AvisoLinter> avisos = new ArrayList<>();

    private String procedimentoAtual = null;
    private int complexidade = 0;

    public VisitorLinter(TabelaSimbolos tabela) {
        this.tabela = tabela;
    }

    public List<AvisoLinter> analisar(Programa programa) {
        verificarVariaveisNaoUtilizadas();
        if (programa != null && programa.getComando() != null) {
            visitarComando(programa.getComando());
        }
        return Collections.unmodifiableList(avisos);
    }

    // ---- Regra 1: variavel nao utilizada ----

    private void verificarVariaveisNaoUtilizadas() {
        for (Escopo escopo : tabela.getTodosEscopos()) {
            for (Simbolo s : escopo.getSimbolos().values()) {
                if (s.getKind() == SimboloKind.VARIAVEL && s.getLido() == 0) {
                    avisos.add(new AvisoLinter(
                            AvisoCodigo.VAR_NAO_UTILIZADA,
                            "variavel '" + s.getNome() + "' declarada mas nunca lida",
                            null, s.getLinha()));
                } else if (s.getKind() == SimboloKind.PARAMETRO && s.getLido() == 0) {
                    avisos.add(new AvisoLinter(
                            AvisoCodigo.PARAM_NAO_UTILIZADO,
                            "parametro '" + s.getNome() + "' declarado mas nunca lido",
                            null, s.getLinha()));
                }
            }
        }
    }

    // ---- Traversal da AST ----

    private void visitarComando(Comando c) {
        if (c instanceof SequenciaComando) {
            SequenciaComando s = (SequenciaComando) c;
            visitarComando(s.getComando1());
            visitarComando(s.getComando2());
        } else if (c instanceof ComandoDeclaracao) {
            ComandoDeclaracao cd = (ComandoDeclaracao) c;
            visitarDeclaracao(cd.getDeclaracao());
            visitarComando(cd.getComando());
        } else if (c instanceof IfThenElse) {
            visitarIfThenElse((IfThenElse) c);
        } else if (c instanceof While) {
            visitarWhile((While) c);
        }
    }

    private void visitarDeclaracao(Declaracao d) {
        if (d instanceof DeclaracaoComposta) {
            DeclaracaoComposta dc = (DeclaracaoComposta) d;
            visitarDeclaracao(dc.getDeclaracao1());
            visitarDeclaracao(dc.getDeclaracao2());
        } else if (d instanceof DeclaracaoProcedimento) {
            visitarDeclaracaoProcedimento((DeclaracaoProcedimento) d);
        }
    }

    // ---- Regra 2: codigo morto ----

    private void visitarIfThenElse(IfThenElse c) {
        if (procedimentoAtual != null) {
            complexidade++;
        }
        Expressao cond = c.getExpressao();
        Boolean valor = avaliador.avaliar(cond);
        if (Boolean.TRUE.equals(valor)) {
            avisos.add(new AvisoLinter(AvisoCodigo.CODIGO_MORTO_RAMO_ELSE,
                    "ramo 'else' inalcancavel porque a condicao e sempre 'true'",
                    null, c.getLinha()));
        } else if (Boolean.FALSE.equals(valor)) {
            avisos.add(new AvisoLinter(AvisoCodigo.CODIGO_MORTO_RAMO_THEN,
                    "ramo 'then' inalcancavel porque a condicao e sempre 'false'",
                    null, c.getLinha()));
        }
        visitarComando(c.getComandoThen());
        visitarComando(c.getComandoElse());
    }

    private void visitarWhile(While c) {
        if (procedimentoAtual != null) {
            complexidade++;
        }
        Boolean valor = avaliador.avaliar(c.getExpressao());
        if (Boolean.FALSE.equals(valor)) {
            avisos.add(new AvisoLinter(AvisoCodigo.CODIGO_MORTO_WHILE,
                    "corpo do 'while' inalcancavel porque a condicao e sempre 'false'",
                    null, c.getLinha()));
        }
        visitarComando(c.getComando());
    }

    // ---- Regra 3: complexidade de procedimento ----

    private void visitarDeclaracaoProcedimento(DeclaracaoProcedimento dp) {
        String nomeProcAnterior = procedimentoAtual;
        int complexidadeAnterior = complexidade;

        procedimentoAtual = dp.getId().getIdName();
        complexidade = 0;

        visitarComando(dp.getDefProcedimento().getComando());

        if (complexidade > LIMIAR_COMPLEXIDADE) {
            avisos.add(new AvisoLinter(
                    AvisoCodigo.COMPLEXIDADE_PROCEDIMENTO,
                    "complexidade ciclomatica " + complexidade + " ultrapassa o limite de " + LIMIAR_COMPLEXIDADE,
                    procedimentoAtual, dp.getLinha()));
        }

        procedimentoAtual = nomeProcAnterior;
        complexidade = complexidadeAnterior;
    }
}
