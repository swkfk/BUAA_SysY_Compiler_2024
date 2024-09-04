package top.swkfk.compiler.frontend.ast.statement;

import top.swkfk.compiler.frontend.ast.ASTNode;

public class Stmt extends ASTNode {

    public enum Type {
        Assign, Expr, Block, If, For, Break, Continue, GetInt, Printf;
    }

    @Override
    protected String getName() {
        return "<Stmt>";
    }
}
