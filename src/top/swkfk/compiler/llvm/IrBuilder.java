package top.swkfk.compiler.llvm;

import top.swkfk.compiler.frontend.ast.CompileUnit;
import top.swkfk.compiler.frontend.ast.declaration.function.FuncDef;
import top.swkfk.compiler.frontend.ast.declaration.function.FuncFormalParam;
import top.swkfk.compiler.frontend.ast.declaration.object.ConstDecl;
import top.swkfk.compiler.frontend.ast.declaration.object.Decl;
import top.swkfk.compiler.frontend.ast.declaration.object.VarDecl;
import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.llvm.value.Block;
import top.swkfk.compiler.llvm.value.Function;
import top.swkfk.compiler.llvm.value.GlobalVariable;
import top.swkfk.compiler.llvm.value.Value;

import java.util.LinkedList;
import java.util.List;

/**
 * It was to be like that: Traverser use this builder to build the IR module. However, for the
 * global variables, it is the builder that traverses the AST to build them. For functions, the
 * builder only calls the traverser to visit them. This is a design flaw.
 */
public class IrBuilder {
    private final Traverser traverser;

    private final List<Function> functions;
    private final List<GlobalVariable> globalVariables;

    private Block insertPoint;

    public IrBuilder(CompileUnit ast) {
        this.traverser = new Traverser(ast, this);
        this.functions = new LinkedList<>();
        this.globalVariables = new LinkedList<>();
    }

    public IrBuilder build() {
        buildGlobalVariables();
        buildFunctions();
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

    private void buildFunctions() {
        for (FuncDef funcDef : traverser.getFunctions()) {
            traverser.visitFunction(funcDef);
        }
    }

    void registerFunction(String name, SymbolType type, List<FuncFormalParam> params) {
        Value.counter.reset();
        Function function = new Function(name, type);
        functions.add(function);

        for (FuncFormalParam param : params) {
            param.getSymbol().setValue(function.addParam(param.getSymbol().getType()));
            param.getSymbol().setFromParam();
        }

        Block entry = new Block(function);
        function.addBlock(entry);

        insertPoint = entry;
    }

    public IrModule emit() {
        return new IrModule(functions, globalVariables);
    }
}
