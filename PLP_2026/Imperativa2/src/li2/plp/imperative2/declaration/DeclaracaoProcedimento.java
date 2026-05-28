package li2.plp.imperative2.declaration;

import li2.plp.expressions2.expression.Id;
import li2.plp.expressions2.memory.IdentificadorJaDeclaradoException;
import li2.plp.expressions2.memory.IdentificadorNaoDeclaradoException;
import li2.plp.imperative1.declaration.Declaracao;
import li2.plp.imperative1.memory.AmbienteCompilacaoImperativa;
import li2.plp.imperative1.memory.AmbienteExecucaoImperativa;
import li2.plp.imperative1.memory.EntradaVaziaException;
import li2.plp.imperative2.memory.AmbienteExecucaoImperativa2;

public class DeclaracaoProcedimento extends Declaracao {

	private Id id;
	private DefProcedimento defProcedimento;
	private int linha;

	public DeclaracaoProcedimento(Id id, DefProcedimento defProcedimento) {
		this(id, defProcedimento, -1);
	}

	public DeclaracaoProcedimento(Id id, DefProcedimento defProcedimento, int linha) {
		super();
		this.id = id;
		this.defProcedimento = defProcedimento;
		this.linha = linha;
	}

	public int getLinha() {
		return linha;
	}

	@Override
	public AmbienteExecucaoImperativa elabora(
			AmbienteExecucaoImperativa ambiente)
			throws IdentificadorJaDeclaradoException,
			IdentificadorNaoDeclaradoException, EntradaVaziaException {
		((AmbienteExecucaoImperativa2) ambiente).mapProcedimento(getId(),
				getDefProcedimento());
		return ambiente;
	}

	public Id getId() {
		return this.id;
	}

	@Override
	public boolean checaTipo(AmbienteCompilacaoImperativa ambiente)
			throws IdentificadorJaDeclaradoException,
			IdentificadorNaoDeclaradoException, EntradaVaziaException {
		boolean resposta;

		ambiente.map(id, defProcedimento.getTipo());

		ListaDeclaracaoParametro parametrosFormais = getDefProcedimento()
				.getParametrosFormais();
		if (parametrosFormais.checaTipo(ambiente)) {
			ambiente.incrementa();
			ambiente = parametrosFormais.elabora(ambiente);
			resposta = getDefProcedimento().getComando().checaTipo(ambiente);
			ambiente.restaura();
		} else {
			resposta = false;
		}
		return resposta;
	}

	public DefProcedimento getDefProcedimento() {
		return this.defProcedimento;
	}
}
