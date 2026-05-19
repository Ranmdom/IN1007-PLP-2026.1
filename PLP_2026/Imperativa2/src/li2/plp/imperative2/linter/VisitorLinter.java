package li2.plp.imperative2.linter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import li2.plp.imperative2.Programa;
import li2.plp.imperative2.semantica.Escopo;
import li2.plp.imperative2.semantica.Simbolo;
import li2.plp.imperative2.semantica.SimboloKind;
import li2.plp.imperative2.semantica.TabelaSimbolos;

public class VisitorLinter {

    private final TabelaSimbolos tabela;
    private final List<AvisoLinter> avisos = new ArrayList<>();

    public VisitorLinter(TabelaSimbolos tabela) {
        this.tabela = tabela;
    }

    public List<AvisoLinter> analisar(Programa programa) {
        verificarVariaveisNaoUtilizadas();
        return Collections.unmodifiableList(avisos);
    }

    private void verificarVariaveisNaoUtilizadas() {
        for (Escopo escopo : tabela.getTodosEscopos()) {
            for (Simbolo s : escopo.getSimbolos().values()) {
                if (s.getKind() == SimboloKind.VARIAVEL && s.getLido() == 0) {
                    avisos.add(new AvisoLinter(
                            AvisoCodigo.VAR_NAO_UTILIZADA,
                            "variavel '" + s.getNome() + "' declarada mas nunca lida"));
                } else if (s.getKind() == SimboloKind.PARAMETRO && s.getLido() == 0) {
                    avisos.add(new AvisoLinter(
                            AvisoCodigo.PARAM_NAO_UTILIZADO,
                            "parametro '" + s.getNome() + "' declarado mas nunca lido"));
                }
            }
        }
    }
}
