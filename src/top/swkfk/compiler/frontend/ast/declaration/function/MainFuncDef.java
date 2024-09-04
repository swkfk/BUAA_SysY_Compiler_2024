package top.swkfk.compiler.frontend.ast.declaration.function;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.block.Block;

final public class MainFuncDef extends ASTNode {
    private final Block block;

    public MainFuncDef(Block block) {
        this.block = block;
    }

    @Override
    protected String getName() {
        return "<MainFuncDef>";
    }
}
