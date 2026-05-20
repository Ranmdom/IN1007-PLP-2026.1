package li2.plp.imperative2.linter;

import li2.plp.expressions2.expression.ExpAnd;
import li2.plp.expressions2.expression.ExpNot;
import li2.plp.expressions2.expression.ExpOr;
import li2.plp.expressions2.expression.Expressao;
import li2.plp.expressions2.expression.ValorBooleano;

public class AvaliadorConstante {

    /**
     * Tenta determinar o valor booleano de uma expressao em tempo de compilacao.
     * Retorna null quando a expressao contem variaveis ou sub-expressoes
     * nao-constantes .
     */
    public Boolean avaliar(Expressao e) {
        if (e instanceof ValorBooleano) {
            return ((ValorBooleano) e).valor();
        }
        if (e instanceof ExpNot) {
            Boolean v = avaliar(((ExpNot) e).getExp());
            return v == null ? null : !v;
        }
        if (e instanceof ExpAnd) {
            Boolean esq = avaliar(((ExpAnd) e).getEsq());
            Boolean dir = avaliar(((ExpAnd) e).getDir());
            if (esq == null || dir == null) return null;
            return esq && dir;
        }
        if (e instanceof ExpOr) {
            Boolean esq = avaliar(((ExpOr) e).getEsq());
            Boolean dir = avaliar(((ExpOr) e).getDir());
            if (esq == null || dir == null) return null;
            return esq || dir;
        }
        return null;
    }
}
