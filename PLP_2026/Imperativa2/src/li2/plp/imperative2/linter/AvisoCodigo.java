package li2.plp.imperative2.linter;

public enum AvisoCodigo {
    VAR_NAO_UTILIZADA("no-unused-vars"),
    PARAM_NAO_UTILIZADO("no-unused-params"),
    CODIGO_MORTO_RAMO_THEN("no-dead-code-then"),
    CODIGO_MORTO_RAMO_ELSE("no-dead-code-else"),
    CODIGO_MORTO_WHILE("no-dead-code-while"),
    COMPLEXIDADE_PROCEDIMENTO("max-complexity");

    private final String codigo;

    AvisoCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    @Override
    public String toString() {
        return codigo;
    }
}