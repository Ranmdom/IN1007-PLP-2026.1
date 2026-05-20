package li2.plp.imperative2.linter;

public class AvisoLinter {

    private final AvisoCodigo codigo;
    private final String mensagem;
    private final String contexto;

    public AvisoLinter(AvisoCodigo codigo, String mensagem) {
        this(codigo, mensagem, null);
    }

    public AvisoLinter(AvisoCodigo codigo, String mensagem, String contexto) {
        this.codigo = codigo;
        this.mensagem = mensagem;
        this.contexto = contexto;
    }

    public AvisoCodigo getCodigo() {
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
        sb.append("LINT [").append(codigo.name()).append("]:");
        if (contexto != null && !contexto.isEmpty()) {
            sb.append(" (em '").append(contexto).append("')");
        }
        sb.append(" ").append(mensagem);
        return sb.toString();
    }
}
