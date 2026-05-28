package li2.plp.imperative2.linter;

public class AvisoLinter {

    private final AvisoCodigo codigo;
    private final String mensagem;
    private final String contexto;
    private final int linha;

    public AvisoLinter(AvisoCodigo codigo, String mensagem) {
        this(codigo, mensagem, null, -1);
    }

    public AvisoLinter(AvisoCodigo codigo, String mensagem, String contexto) {
        this(codigo, mensagem, contexto, -1);
    }

    public AvisoLinter(AvisoCodigo codigo, String mensagem, String contexto, int linha) {
        this.codigo = codigo;
        this.mensagem = mensagem;
        this.contexto = contexto;
        this.linha = linha;
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

    public int getLinha() {
        return linha;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (linha > 0) {
            sb.append("linha ").append(linha).append(": ");
        }
        sb.append("LINT [").append(codigo.getCodigo()).append("]:");
        if (contexto != null && !contexto.isEmpty()) {
            sb.append(" (em '").append(contexto).append("')");
        }
        sb.append(" ").append(mensagem);
        return sb.toString();
    }
}
