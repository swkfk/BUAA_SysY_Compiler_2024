package top.swkfk.compiler.llvm;

import top.swkfk.compiler.frontend.ast.CompileUnit;
import top.swkfk.compiler.frontend.ast.declaration.function.FuncDef;
import top.swkfk.compiler.frontend.ast.declaration.function.FuncFormalParam;
import top.swkfk.compiler.frontend.ast.declaration.object.ConstDecl;
import top.swkfk.compiler.frontend.ast.declaration.object.Decl;
import top.swkfk.compiler.frontend.ast.declaration.object.VarDecl;
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
import top.swkfk.compiler.llvm.value.instruction.ILoad;
import top.swkfk.compiler.llvm.value.instruction.IStore;
import top.swkfk.compiler.llvm.value.instruction.ITerminator;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * It was to be like that: Traverser use this builder to build the IR module. However, for the
 * global variables, it is the builder that traverses the AST to build them. For functions, the
 * builder only calls the traverser to visit them. This is a design flaw.
 */
final public class IrBuilder {
    private final Traverser traverser;

    @SuppressWarnings("SpellCheckingInspection")
    private final static Map<String, Function> externalFunctions = Map.of(
        "getint", Function.external("getint", Ty.I32),
        "getchar", Function.external("getchar", Ty.I32),
        "putint", Function.external("putint", Ty.Void, Ty.I32),
        "putch", Function.external("putch", Ty.Void, Ty.I32),
        "putstr", Function.external("putstr", Ty.Void, new TyPtr(Ty.I8))
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
        // 构建全局变量，在相应的 def 中标记 value
        buildGlobalVariables();
        buildFunctions();
        return this;
    }

    private void buildGlobalVariables() {
        for (Decl decl : traverser.getGlobalVariables()) {
            // 两者没有本质区别，只是在语法树中的表示不同
            if (decl.getType().equals(Decl.Type.Const)) {
                globalVariables.addAll(((ConstDecl) decl.getDeclaration()).getDefs().stream().map(
                    def -> {
                        // 这一行设置了 def 中 symbol 对应的 ir value，是指针类型！
                        traverser.markGlobalVars(def);
                        // 这里的 GlobalVariable 是 llvm 中的全局变量，只用来输出 IR 代码，不参与具体的构建！
                        return GlobalVariable.from(def);
                    }
                ).toList());
            } else {
                globalVariables.addAll(((VarDecl) decl.getDeclaration()).getDefs().stream().map(
                    def -> {
                        traverser.markGlobalVars(def);
                        return GlobalVariable.from(def);
                    }
                ).toList());
            }
        }
    }

    private void buildFunctions() {
        for (FuncDef funcDef : traverser.getFunctions()) {
            traverser.visitFunction(funcDef);
        }
        // 这里产生了一个临时 FuncDef，用于方便遍历
        traverser.visitFunction(traverser.getMainFunction().into());
    }

    public Function getCurrentFunction() {
        return currentFunction;
    }

    Function registerFunction(String name, SymbolType type, List<FuncFormalParam> params) {
        Function function = new Function(name, type);
        functions.add(function);
        currentFunction = function;

        for (FuncFormalParam param : params) {
            // 将每个参数都产生一个 IR Value，存储在语法树对应的 symbol 中
            param.getSymbol().setValue(function.addParam(param.getSymbol().getType()));
        }

        BasicBlock entry = new BasicBlock(function, "Function Entry");
        function.addBlock(entry);
        insertPoint = entry;

        // 这里是将每一个形式参数都存储在内存中
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

        return function;
    }

    BasicBlock getInsertPoint() {
        return insertPoint;
    }

    /**
     * Create a new block and add it to the current function. WILL change the insert point.
     * @param autoFallThroughInto whether to add a branch instruction in the origin
     *                       insertPoint to jump into the new block
     * @return the new block
     */
    BasicBlock createBlock(boolean autoFallThroughInto, String comment) {
        BasicBlock block = new BasicBlock(currentFunction, comment);
        currentFunction.addBlock(block);
        if (autoFallThroughInto && !(insertPoint.getLastInstruction() instanceof ITerminator)) {
            insertInstruction(
                new IBranch(block)
            );
        }
        insertPoint = block;
        return block;
    }

    Function getExternalFunction(String name) {
        return externalFunctions.get(name);
    }

    User insertInstruction(User instruction) {
        return insertInstruction(insertPoint, instruction);
    }

    User insertInstruction(BasicBlock block, User instruction) {
        // Create a new block will add a direct jump instruction to the old block if there
        // is no terminator in it. But sometimes, we need to insert a terminator instruction
        // after create a new block. So we need to check if the last instruction is a terminator.
        // If it is, we should remove it and add the new terminator instruction.
        // ^^^ 但这里并没有移除前面存在的 terminator，而是直接将其返回，并且不再插入新的语句
        if (block.getLastInstruction() instanceof ITerminator) {
            return instruction;
        }
        block.addInstruction(instruction);
        return instruction;
    }

    void jumpToNewBlock(String comment) {
        BasicBlock block = new BasicBlock(insertPoint.getParent(), comment);
        insertPoint.getParent().addBlock(block);
        insertInstruction(
            new IBranch(block)
        );
        insertPoint = block;
    }

    Value getGep(SymbolVariable symbol, List<Value> indices) {
        // 如果是形式参数，那么需要先加载一下
        Value loaded = symbol.isFromParam() ?
            insertInstruction(new ILoad(symbol.getValue()))
            : symbol.getValue();
        Value res = insertInstruction(
            // gep 函数内部会处理好，但是需要传递一下是否是形式参数
            new IGep(loaded, indices.get(0), symbol.isFromParam())
        );
        // 这里还支持了多维数组，但是今年不会用到
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
