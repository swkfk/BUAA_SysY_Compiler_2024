package top.swkfk.compiler.frontend;

import top.swkfk.compiler.error.ErrorTable;
import top.swkfk.compiler.frontend.ast.CompileUnit;

public class Traverser {
    private final CompileUnit ast;
    private final ErrorTable errors;

    public Traverser(CompileUnit ast, ErrorTable errors) {
        this.ast = ast;
        this.errors = errors;
    }

    public void spawn() {
    }
}
