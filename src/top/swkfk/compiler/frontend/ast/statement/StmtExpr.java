package top.swkfk.compiler.frontend.ast.statement;

import top.swkfk.compiler.frontend.ast.expression.Expr;

final public class StmtExpr extends Stmt {
    /**
     * The expression of the statement. If it is null, it is just a `;`.
     */
    private final Expr expr;

    public StmtExpr(Expr expr) {
        super(Type.Expr);
        this.expr = expr;
    }

    public Expr getExpr() {
        return expr;
    }
}
