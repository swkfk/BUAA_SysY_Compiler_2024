package top.swkfk.compiler.frontend.ast.declaration.function;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.block.Block;
import top.swkfk.compiler.frontend.symbol.SymbolFunction;
import top.swkfk.compiler.frontend.symbol.SymbolVariable;

final public class FuncDef extends ASTNode {
    private final FuncType type;
    private final String identifier;
    private SymbolFunction symbol = null;
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

    public void setSymbol(SymbolFunction symbol) {
        this.symbol = symbol;
    }
}
