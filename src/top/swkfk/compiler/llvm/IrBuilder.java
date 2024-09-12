package top.swkfk.compiler.llvm;

import top.swkfk.compiler.frontend.ast.CompileUnit;
import top.swkfk.compiler.frontend.ast.declaration.object.ConstDecl;
import top.swkfk.compiler.frontend.ast.declaration.object.Decl;
import top.swkfk.compiler.frontend.ast.declaration.object.VarDecl;
import top.swkfk.compiler.llvm.value.Function;
import top.swkfk.compiler.llvm.value.GlobalVariable;

import java.util.LinkedList;
import java.util.List;

public class IrBuilder {
    private final Traverser traverser;

    private final List<Function> functions;
    private final List<GlobalVariable> globalVariables;

    public IrBuilder(CompileUnit ast) {
        this.traverser = new Traverser(ast);
        this.functions = new LinkedList<>();
        this.globalVariables = new LinkedList<>();
    }

    public IrBuilder build() {
        buildGlobalVariables();
        return this;
    }

    private void buildGlobalVariables() {
        for (Decl decl : traverser.getGlobalVariables()) {
            if (decl.getType().equals(Decl.Type.Const)) {
                globalVariables.addAll(((ConstDecl) decl.getDeclaration()).getDefs().stream().map(
                    GlobalVariable::from
                ).toList());
            } else {
                globalVariables.addAll(((VarDecl) decl.getDeclaration()).getDefs().stream().map(
                    GlobalVariable::from
                ).toList());
            }
        }
    }

    public IrModule emit() {
        return new IrModule(functions, globalVariables);
    }
}
