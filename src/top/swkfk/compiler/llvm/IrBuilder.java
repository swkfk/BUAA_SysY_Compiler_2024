package top.swkfk.compiler.llvm;

import top.swkfk.compiler.frontend.ast.CompileUnit;
import top.swkfk.compiler.frontend.ast.declaration.function.FuncDef;
import top.swkfk.compiler.frontend.ast.declaration.function.FuncFormalParam;
import top.swkfk.compiler.frontend.ast.declaration.object.ConstDecl;
import top.swkfk.compiler.frontend.ast.declaration.object.Decl;
import top.swkfk.compiler.frontend.ast.declaration.object.VarDecl;
import top.swkfk.compiler.frontend.symbol.SymbolFunction;
import top.swkfk.compiler.frontend.symbol.SymbolVariable;
import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.frontend.symbol.type.Ty;
import top.swkfk.compiler.frontend.symbol.type.TyPtr;
import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.llvm.value.Function;
import top.swkfk.compiler.llvm.value.GlobalVariable;
import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.llvm.value.instruction.IAllocate;
import top.swkfk.compiler.llvm.value.instruction.IBranch;
import top.swkfk.compiler.llvm.value.instruction.IGep;
import top.swkfk.compiler.llvm.value.instruction.IStore;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * It was to be like that: Traverser use this builder to build the IR module. However, for the
 * global variables, it is the builder that traverses the AST to build them. For functions, the
 * builder only calls the traverser to visit them. This is a design flaw.
 */
public class IrBuilder {
    private final Traverser traverser;

    @SuppressWarnings("SpellCheckingInspection")
    private final static Map<String, SymbolFunction> externalFunctions = Map.of(
        "getint", new SymbolFunction("getint", Ty.I32),
        "putint", new SymbolFunction("putint", Ty.Void) {{
            addParameter(new SymbolVariable("_1_i32", Ty.I32, false));
        }},
        "putch", new SymbolFunction("putch", Ty.Void) {{
            addParameter(new SymbolVariable("_1_i32", Ty.I32, false));
        }},
        "putstr", new SymbolFunction("putstr", Ty.Void) {{
            addParameter(new SymbolVariable("_1_i8_star", new TyPtr(Ty.I8), false));
        }}
    );

    private final List<Function> functions;
    private final List<GlobalVariable> globalVariables;

    private BasicBlock insertPoint;
    private Function currentFunction;

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
        currentFunction = function;

        for (FuncFormalParam param : params) {
            param.getSymbol().setValue(function.addParam(param.getSymbol().getType()));
        }

        BasicBlock entry = new BasicBlock(function);
        function.addBlock(entry);
        insertPoint = entry;

        for (FuncFormalParam param : params) {
            Value raw = param.getSymbol().getValue();
            Value pointer = insertInstruction(
                new IAllocate(new TyPtr(param.getSymbol().getType()))
            );

            insertInstruction(
                new IStore(raw, pointer)
            );

            param.getSymbol().setValue(pointer);
            param.getSymbol().setFromParam();
        }
    }

    void setInsertPoint(BasicBlock block) {
        insertPoint = block;
    }

    BasicBlock getInsertPoint() {
        return insertPoint;
    }

    /**
     * Create a new block and add it to the current function. WILL change the insert point.
     * @return the new block
     */
    BasicBlock createBlock() {
        BasicBlock block = new BasicBlock(currentFunction);
        currentFunction.addBlock(block);
        insertPoint = block;
        return block;
    }

    User insertInstruction(User instruction) {
        return insertInstruction(insertPoint, instruction);
    }

    User insertInstruction(BasicBlock block, User instruction) {
        block.addInstruction(instruction);
        return instruction;
    }

    void jumpToNewBlock() {
        BasicBlock block = new BasicBlock(insertPoint.getParent());
        insertPoint.getParent().addBlock(block);
        insertInstruction(
            new IBranch(block)
        );
        insertPoint = block;
    }

    Value getGep(SymbolVariable symbol, List<Value> indices) {
        Value res = insertInstruction(
            new IGep(symbol.getValue(), indices.get(0), symbol.isFromParam())
        );
        for (int i = 1; i < indices.size(); i++) {
            res = insertInstruction(
                new IGep(res, indices.get(i), false)
            );
        }
        return res;
    }

    public IrModule emit() {
        return new IrModule(functions, externalFunctions, globalVariables);
    }
}
