package top.swkfk.compiler.frontend.ast.misc;

import top.swkfk.compiler.frontend.ast.ASTNode;

final public class Char extends ASTNode {
    private final char value;

    public Char(char value) {
        this.value = value;
    }

    public char getValue() {
        return value;
    }

    @Override
    protected String getName() {
        return "<Character>";
    }
}
