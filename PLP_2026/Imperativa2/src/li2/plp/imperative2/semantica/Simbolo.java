package li2.plp.imperative2.semantica;

import li2.plp.expressions1.util.Tipo;
import li2.plp.imperative2.declaration.ListaDeclaracaoParametro;

/**
 * Representa um simbolo da tabela: variavel, parametro ou procedimento.
 *
 * Os contadores `lido` e `escrito` ja sao mantidos aqui mesmo sem o
 * linter ter sido implementado, porque o visitor semantico ja faz as
 * marcacoes corretas durante a travessia da AST e isso evita ter que
 * voltar nessa classe na Fase 4.
 */
public class Simbolo {

	private final String nome;
	private final Tipo tipo;
	private final SimboloKind kind;
	private final int escopoNivel;
	private final int linha;

	private final ListaDeclaracaoParametro parametrosFormais;

	private int lido = 0;
	private int escrito = 0;

	public Simbolo(String nome, Tipo tipo, SimboloKind kind, int escopoNivel) {
		this(nome, tipo, kind, escopoNivel, null, -1);
	}

	public Simbolo(String nome, Tipo tipo, SimboloKind kind, int escopoNivel,
			ListaDeclaracaoParametro parametrosFormais) {
		this(nome, tipo, kind, escopoNivel, parametrosFormais, -1);
	}

	public Simbolo(String nome, Tipo tipo, SimboloKind kind, int escopoNivel,
			ListaDeclaracaoParametro parametrosFormais, int linha) {
		this.nome = nome;
		this.tipo = tipo;
		this.kind = kind;
		this.escopoNivel = escopoNivel;
		this.parametrosFormais = parametrosFormais;
		this.linha = linha;
	}

	public String getNome() {
		return nome;
	}

	public Tipo getTipo() {
		return tipo;
	}

	public SimboloKind getKind() {
		return kind;
	}

	public int getEscopoNivel() {
		return escopoNivel;
	}

	public ListaDeclaracaoParametro getParametrosFormais() {
		return parametrosFormais;
	}

	public int getLinha() {
		return linha;
	}

	public int getLido() {
		return lido;
	}

	public int getEscrito() {
		return escrito;
	}

	public void incrementarLido() {
		lido++;
	}

	public void incrementarEscrito() {
		escrito++;
	}

	@Override
	public String toString() {
		return kind + " " + nome + ":" + (tipo == null ? "?" : tipo.getNome())
				+ " (escopo=" + escopoNivel + ", lido=" + lido
				+ ", escrito=" + escrito + ")";
	}
}
