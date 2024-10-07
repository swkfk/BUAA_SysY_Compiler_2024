package top.swkfk.compiler.frontend.ast.declaration.function;

import top.swkfk.compiler.frontend.ast.ASTNode;

final public class FuncType extends ASTNode {
    public enum Type {
        Void, Int, Char
    }

    private final Type type;

    public FuncType(Type type) {
        this.type = type;
    }

    public boolean is(Type type) {
        return this.type == type;
    }

    @Override
    protected String getName() {
        return "<FuncType>";
    }
}
