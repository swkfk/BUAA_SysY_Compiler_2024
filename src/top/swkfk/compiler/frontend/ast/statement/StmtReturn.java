package top.swkfk.compiler.frontend.ast.statement;

import top.swkfk.compiler.frontend.ast.expression.Expr;
import top.swkfk.compiler.frontend.token.Token;

final public class StmtReturn extends Stmt {
    /**
     * The expression to return. Null if no expression is present.
     */
    private final Expr expr;
    private final Token token;

    public StmtReturn(Expr expr, Token tk) {
        super(Type.Return);
        this.expr = expr;
        this.token = tk;
    }

    public StmtReturn(Token tk) {
        super(Type.Return);
        this.expr = null;
        this.token = tk;
    }

    public Expr getExpr() {
        return expr;
    }

    public Token getToken() {
        return token;
    }
}
