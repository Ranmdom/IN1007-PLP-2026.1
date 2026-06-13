package li2.plp.functional2.expression;

import li2.plp.expressions1.util.Tipo;
import li2.plp.expressions2.expression.Expressao;
import li2.plp.expressions2.expression.Valor;
import li2.plp.expressions2.memory.AmbienteCompilacao;
import li2.plp.expressions2.memory.AmbienteExecucao;
import li2.plp.expressions2.memory.VariavelJaDeclaradaException;
import li2.plp.expressions2.memory.VariavelNaoDeclaradaException;
import li2.plp.imperative2.visitor.AstVisitor;

public class ValorIrredutivel implements Valor {

	public Valor avaliar(AmbienteExecucao amb)
			throws VariavelNaoDeclaradaException, VariavelJaDeclaradaException {
		return null;
	}

	public boolean checaTipo(AmbienteCompilacao amb)
			throws VariavelNaoDeclaradaException, VariavelJaDeclaradaException {
		return true;
	}

	public Tipo getTipo(AmbienteCompilacao amb)
			throws VariavelNaoDeclaradaException, VariavelJaDeclaradaException {
		return null;
	}

	public Expressao reduzir(AmbienteExecucao ambiente) {
		return this;
	}
	
	public ValorIrredutivel clone() {
		return this;
	}

	/**
	 * Este valor pertence a Linguagem Funcional 2 e nao faz parte da AST
	 * imperativa percorrida pelo {@link AstVisitor}; portanto nao ha
	 * sobrecarga {@code visit} para ele e o accept e um no-op.
	 */
	public void accept(AstVisitor v) {
		// sem dispatch: no fora da AST imperativa
	}
}
