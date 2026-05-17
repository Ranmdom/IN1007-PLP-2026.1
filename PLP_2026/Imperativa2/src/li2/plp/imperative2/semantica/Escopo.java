package li2.plp.imperative2.semantica;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Um escopo da tabela de simbolos. Mantem os simbolos declarados no
 * proprio escopo (sem buscar no pai) e referencia o escopo pai para
 * permitir a resolucao em cadeia feita pela TabelaSimbolos.
 *
 * O escopo guarda os simbolos em LinkedHashMap para preservar a ordem
 * de declaracao, o que ajuda a estabilizar a saida do linter.
 */
public class Escopo {

	private final int nivel;
	private final Escopo pai;
	private final Map<String, Simbolo> simbolos = new LinkedHashMap<>();

	public Escopo(int nivel, Escopo pai) {
		this.nivel = nivel;
		this.pai = pai;
	}

	public int getNivel() {
		return nivel;
	}

	public Escopo getPai() {
		return pai;
	}

	public Map<String, Simbolo> getSimbolos() {
		return Collections.unmodifiableMap(simbolos);
	}

	public boolean declarar(Simbolo simbolo) {
		if (simbolos.containsKey(simbolo.getNome())) {
			return false;
		}
		simbolos.put(simbolo.getNome(), simbolo);
		return true;
	}

	public Simbolo buscarLocal(String nome) {
		return simbolos.get(nome);
	}
}
