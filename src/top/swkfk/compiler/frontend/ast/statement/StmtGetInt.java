package top.swkfk.compiler.frontend.ast.statement;

import top.swkfk.compiler.frontend.ast.misc.LeftValue;

final public class StmtGetInt extends Stmt {
    private final LeftValue left;

    public StmtGetInt(LeftValue left) {
        this.left = left;
    }
}
