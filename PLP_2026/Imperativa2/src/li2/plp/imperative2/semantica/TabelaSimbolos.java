package li2.plp.imperative2.semantica;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tabela de simbolos do analisador semantico. Mantem uma cadeia de
 * escopos ativos (escopo atual + ancestrais) e arquiva os escopos
 * fechados para que o linter da Fase 4 possa inspecionar simbolos
 * que ja sairam de cena (por exemplo, variaveis declaradas em um
 * bloco que nunca foram lidas).
 *
 * Esta tabela e independente do `Contexto<Tipo>` usado pelos metodos
 * `checaTipo()` antigos: ela guarda mais informacao por simbolo (kind,
 * contadores de leitura/escrita, parametros formais de procedimento)
 * e nao e descartada quando o escopo fecha.
 */
public class TabelaSimbolos {

	private Escopo escopoAtual;
	private final List<Escopo> escoposFechados = new ArrayList<>();
	private int proximoNivel = 0;

	public TabelaSimbolos() {
		this.escopoAtual = new Escopo(proximoNivel++, null);
	}

	public void abrirEscopo() {
		this.escopoAtual = new Escopo(proximoNivel++, escopoAtual);
	}

	public void fecharEscopo() {
		if (escopoAtual.getPai() == null) {
			throw new IllegalStateException(
					"tentativa de fechar o escopo global");
		}
		escoposFechados.add(escopoAtual);
		escopoAtual = escopoAtual.getPai();
	}

	public boolean declarar(Simbolo simbolo) {
		return escopoAtual.declarar(simbolo);
	}

	public Simbolo buscar(String nome) {
		Escopo e = escopoAtual;
		while (e != null) {
			Simbolo s = e.buscarLocal(nome);
			if (s != null) {
				return s;
			}
			e = e.getPai();
		}
		return null;
	}

	public Simbolo buscarLocal(String nome) {
		return escopoAtual.buscarLocal(nome);
	}

	public Escopo getEscopoAtual() {
		return escopoAtual;
	}

	public List<Escopo> getEscoposFechados() {
		return Collections.unmodifiableList(escoposFechados);
	}

	/**
	 * Retorna todos os escopos vistos pela tabela: os fechados (em ordem
	 * de fechamento) e o escopo atual no final. Util para o linter ler
	 * todos os simbolos depois que a analise terminou.
	 */
	public List<Escopo> getTodosEscopos() {
		List<Escopo> todos = new ArrayList<>(escoposFechados);
		todos.add(escopoAtual);
		return Collections.unmodifiableList(todos);
	}
}
