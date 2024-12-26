package top.swkfk.compiler.llvm;

import top.swkfk.compiler.frontend.ast.CompileUnit;
import top.swkfk.compiler.frontend.ast.block.Block;
import top.swkfk.compiler.frontend.ast.block.BlockItem;
import top.swkfk.compiler.frontend.ast.declaration.function.FuncDef;
import top.swkfk.compiler.frontend.ast.declaration.function.FuncType;
import top.swkfk.compiler.frontend.ast.declaration.function.MainFuncDef;
import top.swkfk.compiler.frontend.ast.declaration.object.ConstDecl;
import top.swkfk.compiler.frontend.ast.declaration.object.ConstDef;
import top.swkfk.compiler.frontend.ast.declaration.object.ConstInitValue;
import top.swkfk.compiler.frontend.ast.declaration.object.Decl;
import top.swkfk.compiler.frontend.ast.declaration.object.VarDecl;
import top.swkfk.compiler.frontend.ast.declaration.object.VarDef;
import top.swkfk.compiler.frontend.ast.declaration.object.VarInitValue;
import top.swkfk.compiler.frontend.ast.expression.Expr;
import top.swkfk.compiler.frontend.ast.expression.ExprAdd;
import top.swkfk.compiler.frontend.ast.expression.ExprMul;
import top.swkfk.compiler.frontend.ast.expression.ExprPrimary;
import top.swkfk.compiler.frontend.ast.expression.ExprUnary;
import top.swkfk.compiler.frontend.ast.expression.ExprUnaryCall;
import top.swkfk.compiler.frontend.ast.expression.ExprUnaryPrimary;
import top.swkfk.compiler.frontend.ast.expression.ExprUnaryUnary;
import top.swkfk.compiler.frontend.ast.logical.Cond;
import top.swkfk.compiler.frontend.ast.logical.CondAnd;
import top.swkfk.compiler.frontend.ast.logical.CondEqu;
import top.swkfk.compiler.frontend.ast.logical.CondOr;
import top.swkfk.compiler.frontend.ast.logical.CondRel;
import top.swkfk.compiler.frontend.ast.misc.Char;
import top.swkfk.compiler.frontend.ast.misc.ForStmt;
import top.swkfk.compiler.frontend.ast.misc.LeftValue;
import top.swkfk.compiler.frontend.ast.misc.Number;
import top.swkfk.compiler.frontend.ast.statement.Stmt;
import top.swkfk.compiler.frontend.ast.statement.StmtAssign;
import top.swkfk.compiler.frontend.ast.statement.StmtBlock;
import top.swkfk.compiler.frontend.ast.statement.StmtExpr;
import top.swkfk.compiler.frontend.ast.statement.StmtFor;
import top.swkfk.compiler.frontend.ast.statement.StmtGetChar;
import top.swkfk.compiler.frontend.ast.statement.StmtGetInt;
import top.swkfk.compiler.frontend.ast.statement.StmtIf;
import top.swkfk.compiler.frontend.ast.statement.StmtPrintf;
import top.swkfk.compiler.frontend.ast.statement.StmtReturn;
import top.swkfk.compiler.frontend.symbol.SymbolVariable;
import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.frontend.symbol.type.Ty;
import top.swkfk.compiler.frontend.symbol.type.TyArray;
import top.swkfk.compiler.frontend.symbol.type.TyPtr;
import top.swkfk.compiler.helpers.Compatibility;
import top.swkfk.compiler.helpers.LoopStorage;
import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.llvm.value.Function;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.llvm.value.constants.ConstInteger;
import top.swkfk.compiler.llvm.value.instruction.BinaryOp;
import top.swkfk.compiler.llvm.value.instruction.IAllocate;
import top.swkfk.compiler.llvm.value.instruction.IBinary;
import top.swkfk.compiler.llvm.value.instruction.IBranch;
import top.swkfk.compiler.llvm.value.instruction.ICall;
import top.swkfk.compiler.llvm.value.instruction.IComparator;
import top.swkfk.compiler.llvm.value.instruction.IConvert;
import top.swkfk.compiler.llvm.value.instruction.ILoad;
import top.swkfk.compiler.llvm.value.instruction.IPhi;
import top.swkfk.compiler.llvm.value.instruction.IReturn;
import top.swkfk.compiler.llvm.value.instruction.IStore;
import top.swkfk.compiler.llvm.value.instruction.ITerminator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

final class Traverser {
    private final CompileUnit ast;
    private final IrBuilder builder;
    private final Map<String, Function> functionMap = new HashMap<>();

    Traverser(CompileUnit ast, IrBuilder builder) {
        this.ast = ast;
        this.builder = builder;
        Compatibility.setBuilder(builder::insertInstruction);
    }

    List<Decl> getGlobalVariables() {
        return ast.getDeclarations();
    }

    List<FuncDef> getFunctions() {
        return ast.getFunctions();
    }

    MainFuncDef getMainFunction() {
        return ast.getMainFunc();
    }

    void markGlobalVars(VarDef def) {
        def.getSymbol().setValue(new Value("@" + def.getSymbol().getName(), new TyPtr(def.getSymbol().getType())));
    }

    void markGlobalVars(ConstDef def) {
        def.getSymbol().setValue(new Value("@" + def.getSymbol().getName(), new TyPtr(def.getSymbol().getType())));
    }

    void visitFunction(FuncDef funcDef) {
        // 注册函数
        Function function = builder.registerFunction(
            funcDef.getSymbol().getName(), funcDef.getSymbol().getType(), funcDef.getParams()
        );
        // 函数名到函数的映射
        functionMap.put(funcDef.getSymbol().getName(), function);
        // 创建函数第一个基本块（除了处理参数的入口块）
        builder.jumpToNewBlock("First Block of Function `" + funcDef.getSymbol().getName() + "`");
        // 访问函数体
        visitBlock(funcDef.getBody());
        // 如果函数没有返回值，且最后一个指令不是终结指令，插入一个返回指令，确保 LLVM 基本块的正确性
        if (funcDef.getType().is(FuncType.Type.Void) &&
            !(builder.getInsertPoint().getLastInstruction() instanceof ITerminator)) {
            builder.insertInstruction(
                new IReturn()
            );
        }
        // 重置当前函数的计数器
        function.saveCounter(Value.counter.reset());
    }

    void visitDecl(Decl decl) {
        if (decl.getType().equals(Decl.Type.Const)) {
            // 常量声明
            for (ConstDef def : ((ConstDecl) decl.getDeclaration()).getDefs()) {
                // 常量的值是不可变的，但是仍然直接分配空间并赋值
                def.getSymbol().setValue(builder.insertInstruction(
                    new IAllocate(new TyPtr(def.getSymbol().getType()))
                ));
                if (def.getInitial().getType() == ConstInitValue.Type.Initializer) {
                    // 普通初始化值
                    Value pointer = def.getSymbol().getValue();
                    Value value = Compatibility.unityIntoPointer(pointer, def.getSymbol().getConstantValue().getLeft().into())[0];
                    // 这里直接赋值！
                    builder.insertInstruction(
                        new IStore(value, pointer)
                    );
                } else if (def.getInitial().getType() == ConstInitValue.Type.SubInitializer) {
                    // 初始化列表，这里默认只有一维数组！
                    List<ConstInitValue> initializers = def.getInitial().getSubInitializers();
                    int length = def.getIndices().get(0).calculate();
                    // 一个一个赋值，后面全部补充为 0
                    for (int i = 0; i < length; i++) {
                        Value pointer = builder.getGep(def.getSymbol(), List.of(new ConstInteger(i)));
                        Value value = new ConstInteger(
                            (i < initializers.size() ? initializers.get(i).getExpr().calculate() : 0),
                            def.getSymbol().getType().getFinalBaseType()
                        );
                        builder.insertInstruction(
                            new IStore(value, pointer)
                        );
                    }
                } else if (def.getInitial().getType() == ConstInitValue.Type.StringConst) {
                    // 字符串常量作初始值
                    fillStringInitializer(def.getSymbol(), def.getIndices().get(0).calculate(), def.getInitial().getStringConst());
                }
            }
        } else {
            // 变量声明
            for (VarDef def : ((VarDecl) decl.getDeclaration()).getDefs()) {
                // 自然需要分配空间
                def.getSymbol().setValue(builder.insertInstruction(
                    new IAllocate(new TyPtr(def.getSymbol().getType()))
                ));
                // 没有初始化值，直接跳过
                if (def.getInitial() == null) {
                    continue;
                }
                // Handle the initial value
                if (def.getInitial().getType() == VarInitValue.Type.Initializer) {
                    // 只有一个值
                    Value pointer = def.getSymbol().getValue();
                    // visitExpr 会生成计算指令
                    Value value = Compatibility.unityIntoPointer(pointer, visitExpr(def.getInitial().getExpr()))[0];
                    builder.insertInstruction(
                        new IStore(value, pointer)
                    );
                } else if (def.getInitial().getType() == VarInitValue.Type.SubInitializer) {
                    // 初始化列表，这里默认只有一维数组！
                    List<VarInitValue> initializers = def.getInitial().getSubInitializers();
                    int length = def.getIndices().get(0).calculate();
                    for (int i = 0; i < length; i++) {
                        Value pointer = builder.getGep(def.getSymbol(), List.of(new ConstInteger(i)));
                        Expr value;
                        if (i < initializers.size()) {
                            // Pay attention to this line. Assume only one dimension.
                            value = initializers.get(i).getExpr();
                        } else {
                            // 后面全部填充为 0
                            value = new Expr(new ExprAdd(new ExprMul(new ExprUnaryPrimary(new ExprPrimary(new Number(0))))));
                        }
                        builder.insertInstruction(
                            new IStore(Compatibility.unityIntoPointer(pointer, visitExpr(value))[0], pointer)
                        );
                    }
                } else if (def.getInitial().getType() == VarInitValue.Type.StringConst) {
                    // 字符串常量作初始值
                    fillStringInitializer(def.getSymbol(), def.getIndices().get(0).calculate(), def.getInitial().getStringConst());
                }
            }
        }
    }

    private void fillStringInitializer(SymbolVariable symbol, int length, String string) {
        // 填充字符串常量，一个一个赋值，后面全部补充为 0
        for (int i = 0; i < length; i++) {
            Value pointer = builder.getGep(symbol, List.of(new ConstInteger(i)));
            Value value = new ConstInteger(
                (i < string.length() ? string.charAt(i) : 0), Ty.I8
            );
            builder.insertInstruction(
                new IStore(value, pointer)
            );
        }
    }

    Value visitExpr(Expr expr) {
        return visitAddExpr(expr.getExpr());
    }

    Value visitAddExpr(ExprAdd expr) {
        Value now = visitMulExpr(expr.getLeft());
        for (int i = 0; i < expr.getOps().size(); i++) {
            Value right = visitMulExpr(expr.getRights().get(i));
            switch (expr.getOps().get(i)) {
                case ADD -> now = builder.insertInstruction(new IBinary(BinaryOp.ADD, Compatibility.unityIntoInteger(now, right)));
                case SUB -> now = builder.insertInstruction(new IBinary(BinaryOp.SUB, Compatibility.unityIntoInteger(now, right)));
            }
        }
        return now;
    }

    Value visitMulExpr(ExprMul expr) {
        Value now = visitUnaryExpr(expr.getLeft());
        for (int i = 0; i < expr.getOps().size(); i++) {
            Value right = visitUnaryExpr(expr.getRights().get(i));
            switch (expr.getOps().get(i)) {
                case MUL -> now = builder.insertInstruction(new IBinary(BinaryOp.MUL, Compatibility.unityIntoInteger(now, right)));
                case DIV -> now = builder.insertInstruction(new IBinary(BinaryOp.DIV, Compatibility.unityIntoInteger(now, right)));
                case MOD -> now = builder.insertInstruction(new IBinary(BinaryOp.MOD, Compatibility.unityIntoInteger(now, right)));
            }
        }
        return now;
    }

    Value visitUnaryExpr(ExprUnary expr) {
        return switch (expr.getType()) {
            case Unary -> {
                ExprUnaryUnary unary = (ExprUnaryUnary) expr;
                Value value = visitUnaryExpr(unary.getExpr());
                yield switch (unary.getOp()) {
                    case Plus -> value;
                    case Minus -> builder.insertInstruction(new IBinary(BinaryOp.SUB, ConstInteger.zero, Compatibility.unityIntoInteger(value)[0]));
                    case Not -> Compatibility.unityIntoBoolean(
                        builder.insertInstruction(new IComparator(BinaryOp.Eq, new ConstInteger(0, value.getType()), value))
                    )[0];
                };
            }
            case Primary -> visitPrimaryExpr(((ExprUnaryPrimary) expr).getPrimary());
            case Call -> visitCall((ExprUnaryCall) expr);
        };
    }

    Value visitPrimaryExpr(ExprPrimary expr) {
        return switch (expr.getType()) {
            case Number -> new ConstInteger(((Number) expr.getValue()).getValue());
            case Expr -> visitExpr((Expr) expr.getValue());
            case Character -> new ConstInteger(((Char) expr.getValue()).getValue(), Ty.I8);
            case LVal -> {
                // 访问一个左值
                LeftValue lVal = (LeftValue) expr.getValue();
                if (lVal.getIndices().isEmpty()) {
                    // 这一个分支处理的是没有索引的情况
                    if (lVal.getSymbol().getType().is("array")) {
                        // 但是是一个数组
                        yield builder.getGep(
                            // Pay attention to this line
                            lVal.getSymbol(), List.of(new ConstInteger(0))
                        );
                    } else {
                        // 不是数组，直接 load 即可
                        yield builder.insertInstruction(
                            new ILoad(lVal.getSymbol().getValue())
                        );
                    }
                } else {
                    // 有索引的情况
                    // 通过 gep 指令获取到地址
                    Value pointer = builder.getGep(
                        lVal.getSymbol(), lVal.getIndices().stream().map(this::visitExpr).toList()
                    );
                    // 这里理论上兼容多维数组，但是今年不会用到
                    int realDim = 0;
                    SymbolType realType;
                    if (lVal.getSymbol().getType().is("ptr")) {
                        realDim++;
                        realType = ((TyPtr) lVal.getSymbol().getType()).getBase();
                    } else {
                        realType = lVal.getSymbol().getType();
                    }
                    while (realType.is("array")) {
                        realDim++;
                        realType = ((TyArray) realType).getBase();
                    }
                    // 判断一下是不是最后一维，如果是，直接 load
                    if (realDim == lVal.getIndices().size()) {
                        yield builder.insertInstruction(
                            new ILoad(pointer)
                        );
                    } else {
                        yield pointer;
                    }
                }
            }
        };
    }

    /**
     * Visit a call expression. Return null if the call is a void function.
     * @param call The call expression.
     * @return The return value of the call.
     */
    Value visitCall(ExprUnaryCall call) {
        Value ret = builder.insertInstruction(
            new ICall(functionMap.get(call.getSymbol().getName()), call.getParams().stream().map(this::visitExpr).toList())
        );
        // 注意，可能返回 null！代表这是一个 void 函数
        return call.getSymbol().getType().is("void") ? null : ret;
    }

    void visitBlock(Block block) {
        block.getItems().forEach(this::visitBlockItem);
    }

    void visitBlockItem(BlockItem item) {
        if (item.getType().equals(BlockItem.Type.Decl)) {
            visitDecl((Decl) item.getItem());
        } else {
            visitStmt((Stmt) item.getItem());
        }
    }

    Value visitCondRel(CondRel rel) {
        Value ret = visitAddExpr(rel.getLeft());
        for (int i = 0; i < rel.getOps().size(); i++) {
            Value right = visitAddExpr(rel.getRights().get(i));
            switch (rel.getOps().get(i)) {
                case Lt -> ret = builder.insertInstruction(new IComparator(BinaryOp.Lt, Compatibility.unityIntoInteger(ret, right)));
                case Gt -> ret = builder.insertInstruction(new IComparator(BinaryOp.Gt, Compatibility.unityIntoInteger(ret, right)));
                case Le -> ret = builder.insertInstruction(new IComparator(BinaryOp.Le, Compatibility.unityIntoInteger(ret, right)));
                case Ge -> ret = builder.insertInstruction(new IComparator(BinaryOp.Ge, Compatibility.unityIntoInteger(ret, right)));
            }
        }
        return ret;
    }

    Value visitCondEqu(CondEqu equ) {
        Value ret = visitCondRel(equ.getLeft());
        for (int i = 0; i < equ.getOps().size(); i++) {
            Value right = visitCondRel(equ.getRights().get(i));
            switch (equ.getOps().get(i)) {
                case Eq -> ret = builder.insertInstruction(new IComparator(BinaryOp.Eq, Compatibility.unityIntoInteger(ret, right)));
                case Ne -> ret = builder.insertInstruction(new IComparator(BinaryOp.Ne, Compatibility.unityIntoInteger(ret, right)));
            }
        }
        return ret;
    }

    private void fillShortcutBranch(BasicBlock currentBlock, List<BasicBlock> intermediateBlocks) {
        BasicBlock mergeBlock = builder.createBlock(false, "Shortcut Merge Block");
        builder.insertInstruction(currentBlock, new IBranch(mergeBlock));
        // 将每个块的最后一条指令的跳转目标设置为 mergeBlock
        for (BasicBlock block : intermediateBlocks) {
            if (block.getLastInstruction() instanceof IBranch branch) {
                branch.fillNullBlock(mergeBlock);
            }
        }
    }

    Value visitCondAnd(CondAnd and) {
        List<CondEqu> list = and.getCondEquList();
        if (list.size() == 1) {
            return visitCondEqu(list.get(0));
        }
        List<BasicBlock> intermediateBlocks = new LinkedList<>();
        // 每一个条件都是一个基本块，最后一个条件的结果是最终结果
        BasicBlock currentBlock = builder.getInsertPoint();
        for (CondEqu equExpr : list) {
            Value cond = Compatibility.unityIntoBoolean(visitCondEqu(equExpr))[0];
            currentBlock = builder.getInsertPoint();
            BasicBlock nextBlock = builder.createBlock(false, "Shortcut Block for And");
            builder.insertInstruction(
                // 汇聚的基本块暂时留空
                currentBlock, new IBranch(cond, nextBlock, null)
            );
            intermediateBlocks.add(currentBlock);
            currentBlock = nextBlock;
        }
        // 最后一个块不要忘了跳转
        fillShortcutBranch(currentBlock, intermediateBlocks);
        // 此时，插入点已经是 mergeBlock 了
        IPhi phi = new IPhi(Ty.I1);
        for (BasicBlock block : intermediateBlocks) {
            phi.addIncoming(block, ConstInteger.logicZero);
        }
        // 最后一个块的结果
        phi.addIncoming(currentBlock, ConstInteger.logicOne);
        // Now the insert point is the merge block
        builder.insertInstruction(phi);
        return phi;
    }

    Value visitCondOr(CondOr or) {
        List<CondAnd> list = or.getCondAndList();
        if (list.size() == 1) {
            return visitCondAnd(list.get(0));
        }
        List<BasicBlock> intermediateBlocks = new LinkedList<>();
        BasicBlock currentBlock = builder.getInsertPoint();
        for (CondAnd andExpr : list) {
            Value cond = Compatibility.unityIntoBoolean(visitCondAnd(andExpr))[0];
            currentBlock = builder.getInsertPoint();
            BasicBlock nextBlock = builder.createBlock(false, "Shortcut Block for Or");
            builder.insertInstruction(
                currentBlock, new IBranch(cond, null, nextBlock)
            );
            intermediateBlocks.add(currentBlock);
            currentBlock = nextBlock;
        }
        fillShortcutBranch(currentBlock, intermediateBlocks);
        // Insert the phi operator
        IPhi phi = new IPhi(Ty.I1);
        for (BasicBlock block : intermediateBlocks) {
            phi.addIncoming(block, ConstInteger.logicOne);
        }
        phi.addIncoming(currentBlock, ConstInteger.logicZero);
        // Now the insert point is the merge block
        builder.insertInstruction(phi);
        return phi;
    }

    Value visitCond(Cond cond) {
        return visitCondOr(cond.getCondOr());
    }

    private final Stack<LoopStorage> localLoops = new Stack<>();

    @SuppressWarnings("SpellCheckingInspection")
    void visitStmt(Stmt stmt) {
        switch (stmt.getType()) {
            case If -> {
                String nav = " @@" + ((StmtIf) stmt).getNavigation().toString();
                // 条件可以直接在现有块中生成
                Value cond = Compatibility.unityIntoBoolean(visitCond(((StmtIf) stmt).getCondition()))[0];
                // 条件很复杂，插入点可能会变化
                BasicBlock originBlock = builder.getInsertPoint();
                // then 分支，总是有的
                BasicBlock thenBlock = builder.createBlock(false, "If Then Block" + nav);
                visitStmt(((StmtIf) stmt).getThenStmt());
                BasicBlock thenEndBlock = builder.getInsertPoint();
                // 此时还没有创建 mergeBlock，因为不知道是否有 else 分支
                BasicBlock mergeBlock;
                if (((StmtIf) stmt).getElseStmt() != null) {
                    // else 分支
                    BasicBlock elseBlock = builder.createBlock(false, "If Else Block" + nav);
                    visitStmt(((StmtIf) stmt).getElseStmt());
                    BasicBlock elseEndBlock = builder.getInsertPoint();
                    mergeBlock = builder.createBlock(false, "If Merge Block" + nav);
                    // 插入条件跳转
                    builder.insertInstruction(
                        originBlock, new IBranch(cond, thenBlock, elseBlock)
                    );
                    // 汇总到 mergeBlock
                    builder.insertInstruction(thenEndBlock, new IBranch(mergeBlock));
                    builder.insertInstruction(elseEndBlock, new IBranch(mergeBlock));
                } else {
                    // 直接新建 mergeBlock 即可
                    mergeBlock = builder.createBlock(false, "If Merge Block" + nav);
                    builder.insertInstruction(thenEndBlock, new IBranch(mergeBlock));
                    builder.insertInstruction(
                        originBlock, new IBranch(cond, thenBlock, mergeBlock)
                    );
                }
            }
            case For -> {
                StmtFor forStmt = (StmtFor) stmt;
                String nav = " @@" + forStmt.getNavigation().toString();
                // init，直接生成在现有的基本块中
                Optional.ofNullable(forStmt.getInit()).ifPresent(this::visitForStmt);

                // cond，事先生成一个基本块，并且默认跳转进来
                BasicBlock condBlock = builder.createBlock(true, "For Cond Block" + nav);
                localLoops.add(new LoopStorage(condBlock, new LinkedList<>(), new LinkedList<>()));
                Value cond;
                if (forStmt.getCondition() != null) {
                    cond = Compatibility.unityIntoBoolean(visitCond(forStmt.getCondition()))[0];
                } else {
                    cond = ConstInteger.logicOne;
                }
                // 这里很关键！因为条件可能很复杂，插入点可能变化
                BasicBlock condEndBlock = builder.getInsertPoint();

                // body
                BasicBlock bodyBlock = builder.createBlock(false, "For Body Block" + nav);
                // 这里借用了 breaks，同样等待填充末尾块
                localLoops.lastElement().breaks().add((IBranch) builder.insertInstruction(
                    condEndBlock,
                    new IBranch(cond, bodyBlock, null)
                ));
                visitStmt(forStmt.getBody());

                // update
                builder.createBlock(true, "For Update Block" + nav);
                Optional.ofNullable(forStmt.getUpdate()).ifPresent(this::visitForStmt);
                BasicBlock updateEndBlock = builder.getInsertPoint();
                // 更新块无条件跳转到条件块
                builder.insertInstruction(
                    new IBranch(condBlock)
                );

                // after
                // 注意，这里没有 fall through，因为这个块是循环的末尾，都是通过 break 的方式跳出循环的！
                BasicBlock exitBlock = builder.createBlock(false, "For Exit Block" + nav);

                // replace the targets
                localLoops.lastElement().replaceBreak(exitBlock);
                localLoops.lastElement().replaceContinue(updateEndBlock);
                localLoops.pop();
            }
            case Break -> {
                // 这里都是等待填充的，因为暂时不知道跳转的具体位置
                localLoops.lastElement().breaks().add((IBranch) builder.insertInstruction(
                    new IBranch(null)
                ));
                builder.createBlock(false, "Dummy Block for Break");
            }
            case Continue -> {
                localLoops.lastElement().continues().add((IBranch) builder.insertInstruction(
                    new IBranch(null)
                ));
                builder.createBlock(false, "Dummy Block for Continue");
            }
            case Return -> {
                // 注意，返回语句之后的语句，不会被生成
                // 但是，如果生成了新的基本块，那么这个基本块会被插入到函数的最后！
                if (((StmtReturn) stmt).getExpr() == null) {
                    builder.insertInstruction(
                        new IReturn()
                    );
                } else {
                    Value ret = visitExpr(((StmtReturn) stmt).getExpr());
                    if (!builder.getCurrentFunction().getType().equals(ret.getType())) {
                        ret = builder.insertInstruction(new IConvert(builder.getCurrentFunction().getType(), ret));
                    }
                    builder.insertInstruction(new IReturn(ret));
                }
            }
            case Block -> visitBlock(((StmtBlock) stmt).getBlock());
            case Assign -> visitAssign(((StmtAssign) stmt).getLeft(), ((StmtAssign) stmt).getRight());
            case Printf -> {
                // 事先遍历并生成每一个参数
                List<Value> args = ((StmtPrintf) stmt).getArgs().stream().map(this::visitExpr).toList();
                int current = 0;
                String format = ((StmtPrintf) stmt).getFormat();
                for (int i = 0; i < format.length(); i++) {
                    // 逐字符处理
                    if (format.charAt(i) == '%' && i < format.length() - 1 && "dc".indexOf(format.charAt(i + 1)) >= 0) {
                        String function = format.charAt(i + 1) == 'd' ? "putint" : "putch";
                        builder.insertInstruction(
                            new ICall(builder.getExternalFunction(function), List.of(
                                Compatibility.unityIntoInteger(args.get(current++))
                            ))
                        );
                        i++;
                    } else {
                        builder.insertInstruction(
                            new ICall(builder.getExternalFunction("putch"), List.of(new ConstInteger((int) format.charAt(i))))
                        );
                    }
                }
            }
            case GetInt -> performAssign(((StmtGetInt) stmt).getLeft(), builder.insertInstruction(
                new ICall(builder.getExternalFunction("getint"), List.of())
            ));
            case GetChar -> performAssign(((StmtGetChar) stmt).getLeft(),
                Compatibility.unityIntoInteger(Ty.I8, builder.insertInstruction(
                    new ICall(builder.getExternalFunction("getchar"), List.of())
                ))[0]
            );
            case Expr -> Optional.ofNullable(((StmtExpr) stmt).getExpr()).ifPresent(this::visitExpr);
        }
    }

    void visitForStmt(ForStmt forStmt) {
        visitAssign(forStmt.getLeft(), forStmt.getRight());
    }

    void visitAssign(LeftValue left, Expr right) {
        performAssign(left, visitExpr(right));
    }

    void performAssign(LeftValue left, Value right) {
        Value pointer;
        if (left.getIndices().isEmpty()) {
            pointer = left.getSymbol().getValue();
        } else {
            pointer = builder.getGep(
                left.getSymbol(), left.getIndices().stream().map(this::visitExpr).toList()
            );
        }
        // 这个方法名称很具有迷惑性，实际上是将 right 统一为 pointer 指向的（整数）类型
        Value value = Compatibility.unityIntoPointer(pointer, right)[0];
        builder.insertInstruction(
            new IStore(value, pointer)
        );
    }
}
