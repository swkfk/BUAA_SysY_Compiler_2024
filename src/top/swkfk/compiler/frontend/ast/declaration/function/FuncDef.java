package top.swkfk.compiler.frontend.ast.declaration.function;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.block.Block;

final public class FuncDef extends ASTNode {
    private final FuncType type;
    private final String identifier;
    private final FuncFormalParams params;
    private final Block block;

    public FuncDef(FuncType type, String identifier, FuncFormalParams params, Block block) {
        this.type = type;
        this.identifier = identifier;
        this.params = params;
        this.block = block;
    }

    @Override
    protected String getName() {
        return "<FuncDef>";
    }
}
