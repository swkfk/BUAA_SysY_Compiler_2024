package top.swkfk.compiler.frontend.ast.misc;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.expression.Expr;

/**
 * Stmt in for header, whose position is strange. I put it in the misc package, in that its name
 * is not <code>StmtFor</code>, but <code>ForStmt</code>.
 */
final public class ForStmt extends ASTNode {
    private final LeftValue left;
    private final Expr right;

    public ForStmt(LeftValue left, Expr right) {
        this.left = left;
        this.right = right;
    }

    public LeftValue getLeft() {
        return left;
    }

    public Expr getRight() {
        return right;
    }

    @Override
    protected String getName() {
        return "<ForStmt>";
    }
}
