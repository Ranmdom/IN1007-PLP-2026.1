package li2.plp.imperative2.visitor;

import li2.plp.expressions2.expression.ExpAnd;
import li2.plp.expressions2.expression.ExpConcat;
import li2.plp.expressions2.expression.ExpDeclaracao;
import li2.plp.expressions2.expression.ExpEquals;
import li2.plp.expressions2.expression.ExpLength;
import li2.plp.expressions2.expression.ExpMenos;
import li2.plp.expressions2.expression.ExpNot;
import li2.plp.expressions2.expression.ExpOr;
import li2.plp.expressions2.expression.ExpSoma;
import li2.plp.expressions2.expression.ExpSub;
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
import li2.plp.imperative2.command.ChamadaProcedimento;
import li2.plp.imperative2.declaration.DeclaracaoProcedimento;

/**
 * Visitor da AST da Linguagem Imperativa 2 no padrao classico de
 * dupla-dispatch (Opcao B do documento de design): cada no concreto
 * implementa {@code accept(AstVisitor v)} e delega para a sobrecarga
 * {@code visit} correspondente ao seu tipo estatico.
 *
 * <p>Todos os metodos {@code visit} tem implementacao default vazia, de
 * modo que um visitor concreto (semantico, linter, avaliador de
 * constantes) so precisa sobrescrever os nos que lhe interessam. Os nos
 * nao sobrescritos sao simplesmente ignorados, reproduzindo a semantica
 * antiga das cadeias de {@code instanceof} (que tambem ignoravam nos
 * desconhecidos).
 */
public interface AstVisitor {

	// ---------- comandos ----------

	default void visit(Skip c) {
	}

	default void visit(SequenciaComando c) {
	}

	default void visit(ComandoDeclaracao c) {
	}

	default void visit(IfThenElse c) {
	}

	default void visit(While c) {
	}

	default void visit(Atribuicao c) {
	}

	default void visit(Write c) {
	}

	default void visit(Read c) {
	}

	default void visit(ChamadaProcedimento c) {
	}

	// ---------- declaracoes ----------

	default void visit(DeclaracaoComposta d) {
	}

	default void visit(DeclaracaoVariavel d) {
	}

	default void visit(DeclaracaoProcedimento d) {
	}

	// ---------- expressoes ----------

	default void visit(ValorInteiro e) {
	}

	default void visit(ValorBooleano e) {
	}

	default void visit(ValorString e) {
	}

	default void visit(Id e) {
	}

	default void visit(ExpDeclaracao e) {
	}

	default void visit(ExpSoma e) {
	}

	default void visit(ExpSub e) {
	}

	default void visit(ExpAnd e) {
	}

	default void visit(ExpOr e) {
	}

	default void visit(ExpConcat e) {
	}

	default void visit(ExpEquals e) {
	}

	default void visit(ExpMenos e) {
	}

	default void visit(ExpNot e) {
	}

	default void visit(ExpLength e) {
	}
}
