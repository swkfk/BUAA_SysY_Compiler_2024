package top.swkfk.compiler.llvm;

import top.swkfk.compiler.frontend.ast.ASTNode;

public class IrBuilder {
    private final ASTNode ast;
    private final IrModule module;

    public IrBuilder(ASTNode ast) {
        this.ast = ast;
        this.module = new IrModule();
    }

    public IrBuilder build() {
        return this;
    }

    public IrModule emit() {
        return module;
    }
}
