package top.swkfk.compiler.frontend.ast.logical;

import top.swkfk.compiler.frontend.ast.ASTNode;

final public class Cond extends ASTNode {
    private final CondOr condOr;

    public Cond(CondOr condOr) {
        this.condOr = condOr;
    }

    public CondOr getCondOr() {
        return condOr;
    }

    @Override
    protected String getName() {
        return "<Cond>";
    }
}
