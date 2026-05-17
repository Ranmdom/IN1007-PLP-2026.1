package li2.plp.imperative2.semantica;

/**
 * Erro produzido pelo VisitorSemantico. Mantem codigo (para categorizar),
 * mensagem (texto legivel) e um contexto opcional (por exemplo, o nome
 * do procedimento ou do bloco onde o erro foi detectado).
 *
 * O formato textual e padronizado e separa visualmente erro semantico
 * de aviso de linter (que sera prefixado com "LINT" na Fase 4).
 */
public class ErroSemantico {

	public enum Codigo {
		IDENTIFICADOR_NAO_DECLARADO,
		IDENTIFICADOR_JA_DECLARADO,
		IDENTIFICADOR_NAO_E_VARIAVEL,
		IDENTIFICADOR_NAO_E_PROCEDIMENTO,
		TIPO_INCOMPATIVEL_ATRIBUICAO,
		TIPO_CONDICAO_NAO_BOOLEANO,
		TIPO_OPERADOR_INCOMPATIVEL,
		ARIDADE_PROCEDIMENTO,
		TIPO_ARGUMENTO_PROCEDIMENTO
	}

	private final Codigo codigo;
	private final String mensagem;
	private final String contexto;

	public ErroSemantico(Codigo codigo, String mensagem) {
		this(codigo, mensagem, null);
	}

	public ErroSemantico(Codigo codigo, String mensagem, String contexto) {
		this.codigo = codigo;
		this.mensagem = mensagem;
		this.contexto = contexto;
	}

	public Codigo getCodigo() {
		return codigo;
	}

	public String getMensagem() {
		return mensagem;
	}

	public String getContexto() {
		return contexto;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ERRO SEMANTICO [").append(codigo.name()).append("]: ")
				.append(mensagem);
		if (contexto != null && !contexto.isEmpty()) {
			sb.append(" (").append(contexto).append(")");
		}
		return sb.toString();
	}
}
