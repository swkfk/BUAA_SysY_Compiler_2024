package top.swkfk.compiler.llvm;

import top.swkfk.compiler.Controller;
import top.swkfk.compiler.frontend.ast.CompileUnit;
import top.swkfk.compiler.frontend.ast.declaration.object.Decl;
import top.swkfk.compiler.frontend.symbol.SymbolTable;

import java.util.List;

class Traverser {
    private final CompileUnit ast;
    private final SymbolTable symbols = Controller.symbols;

    Traverser(CompileUnit ast) {
        this.ast = ast;
    }

    List<Decl> getGlobalVariables() {
        return ast.getDeclarations();
    }

}
