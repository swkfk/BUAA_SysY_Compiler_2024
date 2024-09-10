package top.swkfk.compiler.frontend.ast.expression;

import top.swkfk.compiler.frontend.ast.ASTNode;

final public class ExprConst extends ASTNode {
    private final ExprAdd expr;

    public ExprConst(ExprAdd expr) {
        this.expr = expr;
    }

    public ExprAdd getExpr() {
        return expr;
    }

    @Override
    protected String getName() {
        return "<ConstExp>";
    }

    public int calculate() {
        return expr.calculateConst();
    }
}
