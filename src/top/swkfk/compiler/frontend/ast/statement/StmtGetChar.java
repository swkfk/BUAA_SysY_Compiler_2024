package top.swkfk.compiler.frontend.ast.statement;

import top.swkfk.compiler.frontend.ast.misc.LeftValue;

final public class StmtGetChar extends Stmt {
    private final LeftValue left;

    public StmtGetChar(LeftValue left) {
        super(Type.GetChar);
        this.left = left;
    }

    public LeftValue getLeft() {
        return left;
    }
}
