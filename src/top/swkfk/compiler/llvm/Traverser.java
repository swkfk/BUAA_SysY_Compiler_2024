package top.swkfk.compiler.llvm;

import top.swkfk.compiler.Controller;
import top.swkfk.compiler.frontend.ast.CompileUnit;
import top.swkfk.compiler.frontend.ast.declaration.function.FuncDef;
import top.swkfk.compiler.frontend.ast.declaration.object.Decl;
import top.swkfk.compiler.frontend.symbol.SymbolTable;

import java.util.List;

class Traverser {
    private final CompileUnit ast;
    private final IrBuilder builder;
    private final SymbolTable symbols = Controller.symbols;

    Traverser(CompileUnit ast, IrBuilder builder) {
        this.ast = ast;
        this.builder = builder;
    }

    List<Decl> getGlobalVariables() {
        return ast.getDeclarations();
    }

    List<FuncDef> getFunctions() {
        return ast.getFunctions();
    }

    void visitFunction(FuncDef funcDef) {
        builder.registerFunction(
            funcDef.getSymbol().getName(), funcDef.getSymbol().getType(), funcDef.getParams()
        );
    }

}
