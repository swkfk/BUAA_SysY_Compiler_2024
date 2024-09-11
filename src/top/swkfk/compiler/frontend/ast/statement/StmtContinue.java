package top.swkfk.compiler.frontend.ast.statement;

import top.swkfk.compiler.frontend.token.Token;

final public class StmtContinue extends Stmt {
    private final Token token;

    public StmtContinue(Token token) {
        super(Type.Continue);
        this.token = token;
    }

    public Token getToken() {
        return token;
    }
}
