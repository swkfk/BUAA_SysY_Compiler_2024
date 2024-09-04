package top.swkfk.compiler.frontend.ast.misc;

import top.swkfk.compiler.frontend.ast.ASTNode;

final public class Number extends ASTNode {
    private final int value;

    public Number(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    protected String getName() {
        return "<Number>";
    }
}
