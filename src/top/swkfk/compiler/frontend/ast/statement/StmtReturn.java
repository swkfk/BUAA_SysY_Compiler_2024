package top.swkfk.compiler.frontend.ast.statement;

import top.swkfk.compiler.frontend.ast.expression.Expr;

final public class StmtReturn extends Stmt {
    /**
     * The expression to return. Null if no expression is present.
     */
    private final Expr expr;

    public StmtReturn(Expr expr) {
        this.expr = expr;
    }

    public StmtReturn() {
        this.expr = null;
    }
}
