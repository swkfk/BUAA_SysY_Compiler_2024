package top.swkfk.compiler.llvm;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.llvm.value.Function;
import top.swkfk.compiler.llvm.value.GlobalVariable;

import java.util.LinkedList;
import java.util.List;

public class IrBuilder {
    private final ASTNode ast;

    private final List<Function> functions;
    private final List<GlobalVariable> globalVariables;

    public IrBuilder(ASTNode ast) {
        this.ast = ast;
        this.functions = new LinkedList<>();
        this.globalVariables = new LinkedList<>();
    }

    public IrBuilder build() {
        return this;
    }

    public IrModule emit() {
        return new IrModule(functions, globalVariables);
    }
}
