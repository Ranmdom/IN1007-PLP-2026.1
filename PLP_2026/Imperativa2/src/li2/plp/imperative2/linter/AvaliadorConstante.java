package li2.plp.imperative2.linter;

import li2.plp.expressions2.expression.ExpAnd;
import li2.plp.expressions2.expression.ExpNot;
import li2.plp.expressions2.expression.ExpOr;
import li2.plp.expressions2.expression.Expressao;
import li2.plp.expressions2.expression.ValorBooleano;
import li2.plp.imperative2.visitor.AstVisitor;

/**
 * Avalia, em tempo de compilacao, se uma expressao booleana e constante.
 *
 * Implementa o padrao Visitor (dupla-dispatch): cada expressao chama
 * {@code accept(this)} e devolve seu valor constante pelo canal
 * {@link #resultado}, lido por {@link #avaliar} logo apos o {@code accept}.
 * Expressoes nao-constantes (ou que envolvem variaveis) caem no default
 * no-op de {@link AstVisitor} e resultam em {@code null}.
 */
public class AvaliadorConstante implements AstVisitor {

    /** Canal de retorno do valor constante calculado por uma visita. */
    private Boolean resultado;

    /**
     * Tenta determinar o valor booleano de uma expressao em tempo de compilacao.
     * Retorna null quando a expressao contem variaveis ou sub-expressoes
     * nao-constantes .
     */
    public Boolean avaliar(Expressao e) {
        if (e == null) {
            return null;
        }
        resultado = null;
        e.accept(this);
        return resultado;
    }

    @Override
    public void visit(ValorBooleano e) {
        resultado = e.valor();
    }

    @Override
    public void visit(ExpNot e) {
        Boolean v = avaliar(e.getExp());
        resultado = v == null ? null : !v;
    }

    @Override
    public void visit(ExpAnd e) {
        Boolean esq = avaliar(e.getEsq());
        Boolean dir = avaliar(e.getDir());
        resultado = (esq == null || dir == null) ? null : (esq && dir);
    }

    @Override
    public void visit(ExpOr e) {
        Boolean esq = avaliar(e.getEsq());
        Boolean dir = avaliar(e.getDir());
        resultado = (esq == null || dir == null) ? null : (esq || dir);
    }
}
