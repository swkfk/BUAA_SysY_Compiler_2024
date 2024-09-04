package top.swkfk.compiler.frontend.ast.expression;

import top.swkfk.compiler.frontend.ast.ASTNode;

public class ExprUnary extends ASTNode {
    public enum Type {
        Primary, Call, Unary
    }

    protected Type type;

    protected ExprUnary(Type type) {
        this.type = type;
    }

    @Override
    protected String getName() {
        return "<UnaryExp>";
    }
}
