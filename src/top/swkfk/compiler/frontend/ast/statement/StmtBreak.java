package top.swkfk.compiler.frontend.ast.statement;

import top.swkfk.compiler.frontend.token.Token;

final public class StmtBreak extends Stmt {
    private final Token token;

    public StmtBreak(Token token) {
        super(Type.Break);
        this.token = token;
    }

    public Token getToken() {
        return token;
    }
}
