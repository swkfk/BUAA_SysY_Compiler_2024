package top.swkfk.compiler.frontend.ast.declaration.function;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.block.Block;
import top.swkfk.compiler.frontend.symbol.SymbolFunction;
import top.swkfk.compiler.frontend.symbol.type.Ty;
import top.swkfk.compiler.frontend.token.Token;

final public class MainFuncDef extends ASTNode {
    private final Token identifier;
    private final Block block;

    public MainFuncDef(Token identifier, Block block) {
        this.identifier = identifier;
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }

    @Override
    protected String getName() {
        return "<MainFuncDef>";
    }

    public FuncDef into() {
        return new FuncDef(new FuncType(FuncType.Type.Int), identifier, new FuncFormalParams(), block) {{
            setSymbol(new SymbolFunction("main", Ty.I32, -1));
        }};
    }
}
