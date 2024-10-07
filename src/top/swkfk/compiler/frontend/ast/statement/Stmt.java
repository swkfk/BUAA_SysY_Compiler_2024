package top.swkfk.compiler.frontend.ast.statement;

import top.swkfk.compiler.frontend.ast.ASTNode;

public class Stmt extends ASTNode {

    public enum Type {
        Assign, Expr, Block, If, For, Break, Continue, GetInt, GetChar, Printf, Return
    }

    private final Type type;

    protected Stmt(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    protected String getName() {
        return "<Stmt>";
    }
}
