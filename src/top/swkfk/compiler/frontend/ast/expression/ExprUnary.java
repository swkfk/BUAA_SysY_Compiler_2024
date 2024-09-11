package top.swkfk.compiler.frontend.ast.expression;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.symbol.type.SymbolType;

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

    abstract public SymbolType calculateType();

    @Override
    protected String getName() {
        return "<UnaryExp>";
    }
}
