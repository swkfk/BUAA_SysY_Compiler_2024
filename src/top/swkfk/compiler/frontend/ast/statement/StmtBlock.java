package top.swkfk.compiler.frontend.ast.statement;

import top.swkfk.compiler.frontend.ast.block.Block;

final public class StmtBlock extends Stmt {
    private final Block block;

    public StmtBlock(Block block) {
        super(Type.Block);
        this.block = block;
    }
}
