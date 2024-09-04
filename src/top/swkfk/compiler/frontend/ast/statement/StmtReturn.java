package top.swkfk.compiler.frontend.ast.statement;

final public class StmtReturn extends Stmt {
    /**
     * The expression to return. Null if no expression is present.
     */
    private final Stmt expr;

    public StmtReturn(Stmt expr) {
        this.expr = expr;
    }

    public StmtReturn() {
        this.expr = null;
    }
}
