package top.swkfk.compiler.frontend.ast.expression;

import top.swkfk.compiler.frontend.ast.ASTNode;

final public class Expr extends ASTNode {

    private final ExprAdd expr;

    public Expr(ExprAdd expr) {
        this.expr = expr;
    }

    @Override
    protected String getName() {
        return "<Exp>";
    }
}
