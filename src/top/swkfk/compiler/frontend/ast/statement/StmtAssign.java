package top.swkfk.compiler.frontend.ast.statement;

import top.swkfk.compiler.frontend.ast.expression.Expr;
import top.swkfk.compiler.frontend.ast.misc.LeftValue;

final public class StmtAssign extends Stmt {
    private final LeftValue left;
    private final Expr right;

    public StmtAssign(LeftValue left, Expr right) {
        this.left = left;
        this.right = right;
    }
}
