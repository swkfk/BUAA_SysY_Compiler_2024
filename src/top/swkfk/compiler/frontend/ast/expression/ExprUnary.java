package top.swkfk.compiler.frontend.ast.expression;

import top.swkfk.compiler.frontend.ast.ASTNode;

abstract public class ExprUnary extends ASTNode {
    public enum Type {
        Primary, Call, Unary
    }

    protected Type type;

    protected ExprUnary(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    abstract public int calculateConst();

    @Override
    protected String getName() {
        return "<UnaryExp>";
    }
}
