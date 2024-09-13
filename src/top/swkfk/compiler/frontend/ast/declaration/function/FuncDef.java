package top.swkfk.compiler.frontend.ast.declaration.function;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.block.Block;
import top.swkfk.compiler.frontend.symbol.SymbolFunction;
import top.swkfk.compiler.frontend.token.Token;

import java.util.List;

final public class FuncDef extends ASTNode {
    private final FuncType type;
    private final Token identifier;
    private SymbolFunction symbol = null;
    private final FuncFormalParams params;
    private final Block block;

    public FuncDef(FuncType type, Token identifier, FuncFormalParams params, Block block) {
        this.type = type;
        this.identifier = identifier;
        this.params = params;
        this.block = block;
    }

    public FuncType getType() {
        return type;
    }

    public Token getIdentifier() {
        return identifier;
    }

    public List<FuncFormalParam> getParams() {
        return params.getParamList();
    }

    public Block getBody() {
        return block;
    }

    @Override
    protected String getName() {
        return "<FuncDef>";
    }

    public void setSymbol(SymbolFunction symbol) {
        this.symbol = symbol;
    }

    public SymbolFunction getSymbol() {
        return symbol;
    }
}
