package top.swkfk.compiler.frontend.ast.declaration.function;

import top.swkfk.compiler.frontend.ast.ASTNode;

final public class FuncType extends ASTNode {
    public enum Type {
        Void, Int
    }

    private Type type;

    public FuncType(Type type) {
        this.type = type;
    }

    @Override
    protected String getName() {
        return "<FuncType>";
    }
}
