package top.swkfk.compiler.frontend;

import top.swkfk.compiler.error.ErrorTable;
import top.swkfk.compiler.frontend.ast.CompileUnit;
import top.swkfk.compiler.frontend.symbol.SymbolTable;

public class Traverser {
    private final CompileUnit ast;
    private final ErrorTable errors;
    private final SymbolTable symbols;

    public Traverser(CompileUnit ast, ErrorTable errors, SymbolTable symbols) {
        this.ast = ast;
        this.errors = errors;
        this.symbols = symbols;
    }

    public void spawn() {
    }
}
