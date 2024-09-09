package top.swkfk.compiler.frontend.ast.statement;

import top.swkfk.compiler.frontend.ast.expression.Expr;

final public class StmtReturn extends Stmt {
    /**
     * The expression to return. Null if no expression is present.
     */
    private final Expr expr;

    public StmtReturn(Expr expr) {
        super(Type.Return);
        this.expr = expr;
    }

    public StmtReturn() {
        super(Type.Return);
        this.expr = null;
    }

    public Expr getExpr() {
        return expr;
    }
}
