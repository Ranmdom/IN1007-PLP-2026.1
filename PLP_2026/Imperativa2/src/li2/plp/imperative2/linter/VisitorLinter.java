package li2.plp.imperative2.linter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import li2.plp.expressions2.expression.Expressao;
import li2.plp.imperative1.command.ComandoDeclaracao;
import li2.plp.imperative1.command.IfThenElse;
import li2.plp.imperative1.command.SequenciaComando;
import li2.plp.imperative1.command.While;
import li2.plp.imperative1.declaration.DeclaracaoComposta;
import li2.plp.imperative2.Programa;
import li2.plp.imperative2.declaration.DeclaracaoProcedimento;
import li2.plp.imperative2.semantica.Escopo;
import li2.plp.imperative2.semantica.Simbolo;
import li2.plp.imperative2.semantica.SimboloKind;
import li2.plp.imperative2.semantica.TabelaSimbolos;
import li2.plp.imperative2.visitor.AstVisitor;

/**
 * Visitor de linter da Linguagem Imperativa 2.
 *
 * Implementa o padrao Visitor classico (dupla-dispatch) sobre a AST:
 * cada no chama {@code accept(this)} e e redirecionado para a sobrecarga
 * {@code visit} correspondente. So sobrescreve os nos relevantes para as
 * regras de lint; os demais usam o comportamento default (no-op) de
 * {@link AstVisitor}, o que reproduz a navegacao seletiva da versao
 * anterior baseada em {@code instanceof}.
 */
public class VisitorLinter implements AstVisitor {

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
            programa.getComando().accept(this);
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

    @Override
    public void visit(SequenciaComando c) {
        c.getComando1().accept(this);
        c.getComando2().accept(this);
    }

    @Override
    public void visit(ComandoDeclaracao c) {
        c.getDeclaracao().accept(this);
        c.getComando().accept(this);
    }

    @Override
    public void visit(DeclaracaoComposta d) {
        d.getDeclaracao1().accept(this);
        d.getDeclaracao2().accept(this);
    }

    // ---- Regra 2: codigo morto ----

    @Override
    public void visit(IfThenElse c) {
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
        c.getComandoThen().accept(this);
        c.getComandoElse().accept(this);
    }

    @Override
    public void visit(While c) {
        if (procedimentoAtual != null) {
            complexidade++;
        }
        Boolean valor = avaliador.avaliar(c.getExpressao());
        if (Boolean.FALSE.equals(valor)) {
            avisos.add(new AvisoLinter(AvisoCodigo.CODIGO_MORTO_WHILE,
                    "corpo do 'while' inalcancavel porque a condicao e sempre 'false'",
                    null, c.getLinha()));
        }
        c.getComando().accept(this);
    }

    // ---- Regra 3: complexidade de procedimento ----

    @Override
    public void visit(DeclaracaoProcedimento dp) {
        String nomeProcAnterior = procedimentoAtual;
        int complexidadeAnterior = complexidade;

        procedimentoAtual = dp.getId().getIdName();
        complexidade = 0;

        dp.getDefProcedimento().getComando().accept(this);

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
