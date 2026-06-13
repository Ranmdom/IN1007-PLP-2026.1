package li2.plp.imperative2.semantica;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import li2.plp.expressions1.util.Tipo;
import li2.plp.expressions1.util.TipoPrimitivo;
import li2.plp.expressions2.expression.ExpAnd;
import li2.plp.expressions2.expression.ExpBinaria;
import li2.plp.expressions2.expression.ExpConcat;
import li2.plp.expressions2.expression.ExpEquals;
import li2.plp.expressions2.expression.ExpLength;
import li2.plp.expressions2.expression.ExpMenos;
import li2.plp.expressions2.expression.ExpNot;
import li2.plp.expressions2.expression.ExpOr;
import li2.plp.expressions2.expression.ExpSoma;
import li2.plp.expressions2.expression.ExpSub;
import li2.plp.expressions2.expression.ExpUnaria;
import li2.plp.expressions2.expression.Expressao;
import li2.plp.expressions2.expression.Id;
import li2.plp.expressions2.expression.ValorBooleano;
import li2.plp.expressions2.expression.ValorInteiro;
import li2.plp.expressions2.expression.ValorString;
import li2.plp.imperative1.command.Atribuicao;
import li2.plp.imperative1.command.ComandoDeclaracao;
import li2.plp.imperative1.command.IfThenElse;
import li2.plp.imperative1.command.Read;
import li2.plp.imperative1.command.SequenciaComando;
import li2.plp.imperative1.command.Skip;
import li2.plp.imperative1.command.While;
import li2.plp.imperative1.command.Write;
import li2.plp.imperative1.declaration.DeclaracaoComposta;
import li2.plp.imperative1.declaration.DeclaracaoVariavel;
import li2.plp.imperative2.Programa;
import li2.plp.imperative2.command.ChamadaProcedimento;
import li2.plp.imperative2.command.ListaExpressao;
import li2.plp.imperative2.declaration.DeclaracaoParametro;
import li2.plp.imperative2.declaration.DeclaracaoProcedimento;
import li2.plp.imperative2.declaration.DefProcedimento;
import li2.plp.imperative2.declaration.ListaDeclaracaoParametro;
import li2.plp.imperative2.util.TipoProcedimento;
import li2.plp.imperative2.visitor.AstVisitor;

/**
 * Visitor semantico da Linguagem Imperativa 2.
 *
 * Implementa o padrao Visitor classico (dupla-dispatch): cada no da AST
 * chama {@code accept(this)} e e redirecionado para a sobrecarga
 * {@code visit} correspondente ao seu tipo concreto. Em vez de lancar
 * excecao no primeiro erro, acumula erros na lista `erros` e segue
 * analisando, para que uma unica execucao reporte o maximo de problemas
 * possivel.
 *
 * O tipo calculado para cada expressao e devolvido pelo canal
 * {@link #resultadoTipo}, lido imediatamente pelo helper {@link #tipoDe}
 * apos cada {@code accept} (a dupla-dispatch das expressoes e sincrona,
 * entao nao ha risco de sobrescrita entre irmaos).
 *
 * A tabela de simbolos e independente do `Contexto<Tipo>` usado pelos
 * `checaTipo()` antigos, e mantem contadores de leitura/escrita por
 * simbolo que serao consumidos pelo linter na Fase 4.
 */
public class VisitorSemantico implements AstVisitor {

	private final TabelaSimbolos tabela = new TabelaSimbolos();
	private final List<ErroSemantico> erros = new ArrayList<>();

	/** Canal de retorno do tipo calculado pela visita de uma expressao. */
	private Tipo resultadoTipo;

	public List<ErroSemantico> analisar(Programa programa) {
		if (programa != null && programa.getComando() != null) {
			programa.getComando().accept(this);
		}
		return Collections.unmodifiableList(erros);
	}

	public TabelaSimbolos getTabela() {
		return tabela;
	}

	public List<ErroSemantico> getErros() {
		return Collections.unmodifiableList(erros);
	}

	// ---------- comandos ----------

	@Override
	public void visit(Skip c) {
		// nada a checar
	}

	@Override
	public void visit(SequenciaComando c) {
		c.getComando1().accept(this);
		c.getComando2().accept(this);
	}

	@Override
	public void visit(ComandoDeclaracao c) {
		tabela.abrirEscopo();
		c.getDeclaracao().accept(this);
		c.getComando().accept(this);
		tabela.fecharEscopo();
	}

	@Override
	public void visit(IfThenElse c) {
		Tipo t = tipoDe(c.getExpressao());
		if (t != null && !t.eBooleano()) {
			registrar(ErroSemantico.Codigo.TIPO_CONDICAO_NAO_BOOLEANO,
					"condicao do 'if' deve ser booleana, mas tem tipo "
							+ nomeTipo(t));
		}
		c.getComandoThen().accept(this);
		c.getComandoElse().accept(this);
	}

	@Override
	public void visit(While c) {
		Tipo t = tipoDe(c.getExpressao());
		if (t != null && !t.eBooleano()) {
			registrar(ErroSemantico.Codigo.TIPO_CONDICAO_NAO_BOOLEANO,
					"condicao do 'while' deve ser booleana, mas tem tipo "
							+ nomeTipo(t));
		}
		c.getComando().accept(this);
	}

	@Override
	public void visit(Atribuicao c) {
		Id id = c.getId();
		Simbolo s = tabela.buscar(id.getIdName());
		Tipo tipoExp = tipoDe(c.getExpressao());

		if (s == null) {
			registrar(ErroSemantico.Codigo.IDENTIFICADOR_NAO_DECLARADO,
					"identificador '" + id.getIdName()
							+ "' nao declarado (atribuicao)");
			return;
		}
		if (s.getKind() == SimboloKind.PROCEDIMENTO) {
			registrar(ErroSemantico.Codigo.IDENTIFICADOR_NAO_E_VARIAVEL,
					"nao e possivel atribuir a '" + id.getIdName()
							+ "': e um procedimento");
			return;
		}
		s.incrementarEscrito();

		if (s.getTipo() != null && tipoExp != null
				&& !s.getTipo().eIgual(tipoExp)) {
			registrar(ErroSemantico.Codigo.TIPO_INCOMPATIVEL_ATRIBUICAO,
					"atribuicao incompativel: '" + id.getIdName()
							+ "' tem tipo " + nomeTipo(s.getTipo())
							+ " mas a expressao tem tipo " + nomeTipo(tipoExp));
		}
	}

	@Override
	public void visit(Write c) {
		tipoDe(c.getExpressao());
	}

	@Override
	public void visit(Read c) {
		Id id = c.getId();
		Simbolo s = tabela.buscar(id.getIdName());
		if (s == null) {
			registrar(ErroSemantico.Codigo.IDENTIFICADOR_NAO_DECLARADO,
					"identificador '" + id.getIdName()
							+ "' nao declarado (read)");
			return;
		}
		if (s.getKind() == SimboloKind.PROCEDIMENTO) {
			registrar(ErroSemantico.Codigo.IDENTIFICADOR_NAO_E_VARIAVEL,
					"'read' aplicado a '" + id.getIdName()
							+ "', que e um procedimento");
			return;
		}
		s.incrementarEscrito();
	}

	@Override
	public void visit(ChamadaProcedimento c) {
		Id nome = c.getNomeProcedimento();
		Simbolo s = tabela.buscar(nome.getIdName());
		ListaExpressao reais = c.getParametrosReais();

		if (s == null) {
			registrar(ErroSemantico.Codigo.IDENTIFICADOR_NAO_DECLARADO,
					"procedimento '" + nome.getIdName() + "' nao declarado");
			// ainda assim, visitar argumentos para nao mascarar erros neles
			visitarTiposDeArgumentos(reais);
			return;
		}
		if (s.getKind() != SimboloKind.PROCEDIMENTO) {
			registrar(ErroSemantico.Codigo.IDENTIFICADOR_NAO_E_PROCEDIMENTO,
					"identificador '" + nome.getIdName()
							+ "' nao e um procedimento");
			visitarTiposDeArgumentos(reais);
			return;
		}

		ListaDeclaracaoParametro formais = s.getParametrosFormais();
		int aridadeFormal = formais == null ? 0 : formais.length();
		int aridadeReal = reais == null ? 0 : reais.length();

		if (aridadeFormal != aridadeReal) {
			registrar(ErroSemantico.Codigo.ARIDADE_PROCEDIMENTO,
					"procedimento '" + nome.getIdName() + "' espera "
							+ aridadeFormal + " argumento(s), recebeu "
							+ aridadeReal);
			visitarTiposDeArgumentos(reais);
			return;
		}

		// aridades batem: caminhar em paralelo
		ListaDeclaracaoParametro fcursor = formais;
		ListaExpressao rcursor = reais;
		int idx = 1;
		while (rcursor != null && rcursor.length() > 0) {
			DeclaracaoParametro paramFormal = fcursor.getHead();
			Expressao argReal = rcursor.getHead();
			Tipo tipoFormal = paramFormal.getTipo();
			Tipo tipoReal = tipoDe(argReal);
			if (tipoFormal != null && tipoReal != null
					&& !tipoFormal.eIgual(tipoReal)) {
				registrar(ErroSemantico.Codigo.TIPO_ARGUMENTO_PROCEDIMENTO,
						"argumento " + idx + " de '" + nome.getIdName()
								+ "' tem tipo " + nomeTipo(tipoReal)
								+ ", esperado " + nomeTipo(tipoFormal));
			}
			fcursor = (ListaDeclaracaoParametro) fcursor.getTail();
			rcursor = (ListaExpressao) rcursor.getTail();
			idx++;
		}
	}

	private void visitarTiposDeArgumentos(ListaExpressao reais) {
		ListaExpressao cursor = reais;
		while (cursor != null && cursor.length() > 0) {
			tipoDe(cursor.getHead());
			cursor = (ListaExpressao) cursor.getTail();
		}
	}

	// ---------- declaracoes ----------

	@Override
	public void visit(DeclaracaoComposta d) {
		d.getDeclaracao1().accept(this);
		d.getDeclaracao2().accept(this);
	}

	@Override
	public void visit(DeclaracaoVariavel d) {
		Id id = d.getId();
		Tipo tipoInicial = tipoDe(d.getExpressao());
		Simbolo simbolo = new Simbolo(id.getIdName(), tipoInicial,
				SimboloKind.VARIAVEL,
				tabela.getEscopoAtual().getNivel(), null, d.getLinha());
		if (!tabela.declarar(simbolo)) {
			registrar(ErroSemantico.Codigo.IDENTIFICADOR_JA_DECLARADO,
					"variavel '" + id.getIdName()
							+ "' ja declarada neste escopo");
		}
	}

	@Override
	public void visit(DeclaracaoProcedimento d) {
		Id id = d.getId();
		DefProcedimento def = d.getDefProcedimento();
		ListaDeclaracaoParametro formais = def.getParametrosFormais();

		Tipo tipoProc = construirTipoProcedimento(formais);
		Simbolo simbolo = new Simbolo(id.getIdName(), tipoProc,
				SimboloKind.PROCEDIMENTO,
				tabela.getEscopoAtual().getNivel(), formais, d.getLinha());
		if (!tabela.declarar(simbolo)) {
			registrar(ErroSemantico.Codigo.IDENTIFICADOR_JA_DECLARADO,
					"procedimento '" + id.getIdName()
							+ "' ja declarado neste escopo");
		}

		// escopo do corpo: parametros + comando
		tabela.abrirEscopo();
		registrarParametros(formais);
		def.getComando().accept(this);
		tabela.fecharEscopo();
	}

	private void registrarParametros(ListaDeclaracaoParametro formais) {
		ListaDeclaracaoParametro cursor = formais;
		while (cursor != null && cursor.length() > 0) {
			DeclaracaoParametro p = cursor.getHead();
			Simbolo s = new Simbolo(p.getId().getIdName(), p.getTipo(),
					SimboloKind.PARAMETRO,
					tabela.getEscopoAtual().getNivel(), null, p.getLinha());
			if (!tabela.declarar(s)) {
				registrar(ErroSemantico.Codigo.IDENTIFICADOR_JA_DECLARADO,
						"parametro '" + p.getId().getIdName()
								+ "' duplicado");
			}
			cursor = (ListaDeclaracaoParametro) cursor.getTail();
		}
	}

	private Tipo construirTipoProcedimento(ListaDeclaracaoParametro formais) {
		List<Tipo> tipos = new ArrayList<>();
		ListaDeclaracaoParametro cursor = formais;
		while (cursor != null && cursor.length() > 0) {
			tipos.add(cursor.getHead().getTipo());
			cursor = (ListaDeclaracaoParametro) cursor.getTail();
		}
		return new TipoProcedimento(tipos);
	}

	// ---------- expressoes ----------

	/**
	 * Calcula o tipo de uma expressao, registrando os erros encontrados
	 * pelo caminho. Retorna null se nao foi possivel determinar o tipo
	 * (por exemplo, identificador nao declarado ou operandos invalidos);
	 * o chamador deve usar essa flag para evitar cascata de erros.
	 *
	 * O resultado e produzido via dupla-dispatch: a visita da expressao
	 * grava em {@link #resultadoTipo}, lido aqui imediatamente apos o
	 * {@code accept}.
	 */
	private Tipo tipoDe(Expressao e) {
		if (e == null) {
			return null;
		}
		resultadoTipo = null;
		e.accept(this);
		return resultadoTipo;
	}

	@Override
	public void visit(ValorInteiro e) {
		resultadoTipo = TipoPrimitivo.INTEIRO;
	}

	@Override
	public void visit(ValorBooleano e) {
		resultadoTipo = TipoPrimitivo.BOOLEANO;
	}

	@Override
	public void visit(ValorString e) {
		resultadoTipo = TipoPrimitivo.STRING;
	}

	@Override
	public void visit(Id e) {
		resultadoTipo = tipoDeId(e);
	}

	@Override
	public void visit(ExpSoma e) {
		resultadoTipo = tipoBinariaPrimitiva(e, TipoPrimitivo.INTEIRO,
				TipoPrimitivo.INTEIRO);
	}

	@Override
	public void visit(ExpSub e) {
		resultadoTipo = tipoBinariaPrimitiva(e, TipoPrimitivo.INTEIRO,
				TipoPrimitivo.INTEIRO);
	}

	@Override
	public void visit(ExpAnd e) {
		resultadoTipo = tipoBinariaPrimitiva(e, TipoPrimitivo.BOOLEANO,
				TipoPrimitivo.BOOLEANO);
	}

	@Override
	public void visit(ExpOr e) {
		resultadoTipo = tipoBinariaPrimitiva(e, TipoPrimitivo.BOOLEANO,
				TipoPrimitivo.BOOLEANO);
	}

	@Override
	public void visit(ExpConcat e) {
		resultadoTipo = tipoBinariaPrimitiva(e, TipoPrimitivo.STRING,
				TipoPrimitivo.STRING);
	}

	@Override
	public void visit(ExpEquals e) {
		resultadoTipo = tipoEquals(e);
	}

	@Override
	public void visit(ExpMenos e) {
		resultadoTipo = tipoUnariaPrimitiva(e, TipoPrimitivo.INTEIRO,
				TipoPrimitivo.INTEIRO);
	}

	@Override
	public void visit(ExpNot e) {
		resultadoTipo = tipoUnariaPrimitiva(e, TipoPrimitivo.BOOLEANO,
				TipoPrimitivo.BOOLEANO);
	}

	@Override
	public void visit(ExpLength e) {
		resultadoTipo = tipoUnariaPrimitiva(e, TipoPrimitivo.STRING,
				TipoPrimitivo.INTEIRO);
	}

	private Tipo tipoDeId(Id id) {
		Simbolo s = tabela.buscar(id.getIdName());
		if (s == null) {
			registrar(ErroSemantico.Codigo.IDENTIFICADOR_NAO_DECLARADO,
					"identificador '" + id.getIdName() + "' nao declarado");
			return null;
		}
		if (s.getKind() == SimboloKind.PROCEDIMENTO) {
			registrar(ErroSemantico.Codigo.IDENTIFICADOR_NAO_E_VARIAVEL,
					"identificador '" + id.getIdName()
							+ "' e procedimento, nao pode ser usado como valor");
			return null;
		}
		s.incrementarLido();
		return s.getTipo();
	}

	private Tipo tipoBinariaPrimitiva(ExpBinaria b, Tipo esperado, Tipo resultado) {
		Tipo te = tipoDe(b.getEsq());
		Tipo td = tipoDe(b.getDir());
		boolean ok = true;
		if (te != null && !te.eIgual(esperado)) {
			registrar(ErroSemantico.Codigo.TIPO_OPERADOR_INCOMPATIVEL,
					"operando esquerdo de '" + b.getOperador() + "' deveria ser "
							+ nomeTipo(esperado) + ", mas e " + nomeTipo(te));
			ok = false;
		}
		if (td != null && !td.eIgual(esperado)) {
			registrar(ErroSemantico.Codigo.TIPO_OPERADOR_INCOMPATIVEL,
					"operando direito de '" + b.getOperador() + "' deveria ser "
							+ nomeTipo(esperado) + ", mas e " + nomeTipo(td));
			ok = false;
		}
		if (te == null || td == null) {
			return null;
		}
		return ok ? resultado : null;
	}

	private Tipo tipoEquals(ExpEquals b) {
		Tipo te = tipoDe(b.getEsq());
		Tipo td = tipoDe(b.getDir());
		if (te == null || td == null) {
			return null;
		}
		if (!te.eIgual(td)) {
			registrar(ErroSemantico.Codigo.TIPO_OPERADOR_INCOMPATIVEL,
					"'==' exige operandos do mesmo tipo, mas recebeu "
							+ nomeTipo(te) + " e " + nomeTipo(td));
			return null;
		}
		return TipoPrimitivo.BOOLEANO;
	}

	private Tipo tipoUnariaPrimitiva(ExpUnaria u, Tipo esperado, Tipo resultado) {
		Tipo to = tipoDe(u.getExp());
		if (to == null) {
			return null;
		}
		if (!to.eIgual(esperado)) {
			registrar(ErroSemantico.Codigo.TIPO_OPERADOR_INCOMPATIVEL,
					"operando de '" + u.getOperador() + "' deveria ser "
							+ nomeTipo(esperado) + ", mas e " + nomeTipo(to));
			return null;
		}
		return resultado;
	}

	// ---------- utilitarios ----------

	private void registrar(ErroSemantico.Codigo codigo, String mensagem) {
		erros.add(new ErroSemantico(codigo, mensagem));
	}

	private static String nomeTipo(Tipo t) {
		return t == null ? "?" : t.getNome();
	}
}
