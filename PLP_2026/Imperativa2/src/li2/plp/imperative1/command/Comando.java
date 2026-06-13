package li2.plp.imperative1.command;

/**
 A execucao de um comando ocorre em um determinado ambiente. O
 resultado de tal execucao � a modifica��o deste ambiente, i.e., comandos
 tem efeito colateral.
 */

import li2.plp.imperative1.memory.AmbienteCompilacaoImperativa;
import li2.plp.imperative1.memory.AmbienteExecucaoImperativa;
import li2.plp.imperative1.memory.EntradaVaziaException;
import li2.plp.imperative1.memory.ErroTipoEntradaException;
import li2.plp.expressions2.memory.IdentificadorJaDeclaradoException;
import li2.plp.expressions2.memory.IdentificadorNaoDeclaradoException;
import li2.plp.imperative2.visitor.AstVisitor;

public interface Comando {

	/**
	 * Aceita um visitor da AST, delegando para a sobrecarga
	 * {@code visit} correspondente ao tipo concreto deste comando
	 * (padrao Visitor / dupla-dispatch).
	 */
	void accept(AstVisitor v);

	/**
	 * Executa este comando.
	 * 
	 * @param ambiente
	 *            o ambiente que contem o mapeamento entre identificadores e
	 *            valores.
	 * 
	 * @return o ambiente modificado pela execu��o do comando.
	 * @throws ErroTipoEntradaException 
	 */
	AmbienteExecucaoImperativa executar(AmbienteExecucaoImperativa ambiente)
			throws IdentificadorJaDeclaradoException,
			IdentificadorNaoDeclaradoException, EntradaVaziaException, ErroTipoEntradaException;

	/**
	 * Realiza a verificacao de tipos deste comando.
	 * 
	 * @param ambiente
	 *            o ambiente que contem o mapeamento entre identificadores e
	 *            tipos.
	 * 
	 * @return <code>true</code> se os comando s�o bem tipados;
	 *         <code>false</code> caso contrario.
	 */
	boolean checaTipo(AmbienteCompilacaoImperativa ambiente)
			throws IdentificadorJaDeclaradoException,
			IdentificadorNaoDeclaradoException, EntradaVaziaException;

}
