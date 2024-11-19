package top.swkfk.compiler.llvm;

import top.swkfk.compiler.Controller;
import top.swkfk.compiler.frontend.ast.CompileUnit;
import top.swkfk.compiler.frontend.ast.block.Block;
import top.swkfk.compiler.frontend.ast.block.BlockItem;
import top.swkfk.compiler.frontend.ast.declaration.function.FuncDef;
import top.swkfk.compiler.frontend.ast.declaration.function.FuncType;
import top.swkfk.compiler.frontend.ast.declaration.function.MainFuncDef;
import top.swkfk.compiler.frontend.ast.declaration.object.ConstDecl;
import top.swkfk.compiler.frontend.ast.declaration.object.ConstDef;
import top.swkfk.compiler.frontend.ast.declaration.object.Decl;
import top.swkfk.compiler.frontend.ast.declaration.object.VarDecl;
import top.swkfk.compiler.frontend.ast.declaration.object.VarDef;
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
import top.swkfk.compiler.frontend.symbol.SymbolTable;
import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.frontend.symbol.type.Ty;
import top.swkfk.compiler.frontend.symbol.type.TyArray;
import top.swkfk.compiler.frontend.symbol.type.TyPtr;
import top.swkfk.compiler.helpers.Compatibility;
import top.swkfk.compiler.helpers.LoopStorage;
import top.swkfk.compiler.llvm.value.BasicBlock;
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

class Traverser {
    private final CompileUnit ast;
    private final IrBuilder builder;
    private final SymbolTable symbols = Controller.symbols;

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
        builder.registerFunction(
            funcDef.getSymbol().getName(), funcDef.getSymbol().getType(), funcDef.getParams()
        );
        builder.jumpToNewBlock("First Block of Function `" + funcDef.getSymbol().getName() + "`");
        visitBlock(funcDef.getBody());
        if (funcDef.getType().is(FuncType.Type.Void) &&
            !(builder.getInsertPoint().getLastInstruction() instanceof ITerminator)) {
            builder.insertInstruction(
                new IReturn()
            );
        }
    }

    void visitDecl(Decl decl) {
        if (decl.getType().equals(Decl.Type.Const)) {
            for (ConstDef def : ((ConstDecl) decl.getDeclaration()).getDefs()) {
                def.getSymbol().setValue(builder.insertInstruction(
                    new IAllocate(new TyPtr(def.getSymbol().getType()))
                ));
                if (def.getSymbol().getConstantValue().isLeft()) {
                    builder.insertInstruction(
                        new IStore(def.getSymbol().getConstantValue().getLeft().into(), def.getSymbol().getValue())
                    );
                } else {
                    // TODO
                }
            }
        } else {
            for (VarDef def : ((VarDecl) decl.getDeclaration()).getDefs()) {
                def.getSymbol().setValue(builder.insertInstruction(
                    new IAllocate(new TyPtr(def.getSymbol().getType()))
                ));
                if (def.getInitial() == null) {
                    continue;
                }
                // Handle the initial value
                if (def.getInitial().getExpr() != null) {
                    builder.insertInstruction(
                        new IStore(visitExpr(def.getInitial().getExpr()), def.getSymbol().getValue())
                    );
                } else {
                    // TODO
                }
            }
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
                LeftValue lVal = (LeftValue) expr.getValue();
                if (lVal.getIndices().isEmpty()) {
                    yield builder.insertInstruction(
                        new ILoad(lVal.getSymbol().getValue())
                    );
                } else {
                    Value pointer = builder.getGep(
                        lVal.getSymbol(), lVal.getIndices().stream().map(this::visitExpr).toList()
                    );
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
            new ICall(call.getSymbol(), call.getParams().stream().map(this::visitExpr).toList())
        );
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
        BasicBlock currentBlock = builder.getInsertPoint();
        for (CondEqu equExpr : list) {
            Value cond = Compatibility.unityIntoBoolean(visitCondEqu(equExpr))[0];
            currentBlock = builder.getInsertPoint();
            BasicBlock nextBlock = builder.createBlock(false, "Shortcut Block for And");
            builder.insertInstruction(
                currentBlock, new IBranch(cond, nextBlock, null)
            );
            intermediateBlocks.add(currentBlock);
            currentBlock = nextBlock;
        }
        fillShortcutBranch(currentBlock, intermediateBlocks);
        IPhi phi = new IPhi(Ty.I1);
        for (BasicBlock block : intermediateBlocks) {
            phi.addIncoming(block, ConstInteger.logicZero);
        }
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
                Value cond = Compatibility.unityIntoBoolean(visitCond(((StmtIf) stmt).getCondition()))[0];
                BasicBlock originBlock = builder.getInsertPoint();
                BasicBlock thenBlock = builder.createBlock(false, "If Then Block");
                visitStmt(((StmtIf) stmt).getThenStmt());
                BasicBlock thenEndBlock = builder.getInsertPoint();
                BasicBlock mergeBlock;
                if (((StmtIf) stmt).getElseStmt() != null) {
                    BasicBlock elseBlock = builder.createBlock(false, "If Else Block");
                    visitStmt(((StmtIf) stmt).getElseStmt());
                    BasicBlock elseEndBlock = builder.getInsertPoint();
                    mergeBlock = builder.createBlock(false, "If Merge Block");
                    builder.insertInstruction(
                        originBlock, new IBranch(cond, thenBlock, elseBlock)
                    );
                    builder.insertInstruction(thenEndBlock, new IBranch(mergeBlock));
                    builder.insertInstruction(elseEndBlock, new IBranch(mergeBlock));
                } else {
                    mergeBlock = builder.createBlock(false, "If Merge Block");
                    builder.insertInstruction(thenEndBlock, new IBranch(mergeBlock));
                    builder.insertInstruction(
                        originBlock, new IBranch(cond, thenBlock, mergeBlock)
                    );
                }
            }
            case For -> {
                StmtFor forStmt = (StmtFor) stmt;
                // init
                Optional.ofNullable(forStmt.getInit()).ifPresent(this::visitForStmt);

                // cond
                BasicBlock condBlock = builder.createBlock(true, "For Cond Block");
                localLoops.add(new LoopStorage(condBlock, new LinkedList<>(), new LinkedList<>()));
                Value cond;
                if (forStmt.getCondition() != null) {
                    cond = Compatibility.unityIntoBoolean(visitCond(forStmt.getCondition()))[0];
                } else {
                    cond = ConstInteger.logicOne;
                }
                BasicBlock condEndBlock = builder.getInsertPoint();

                // body
                BasicBlock bodyBlock = builder.createBlock(false, "For Body Block");
                localLoops.lastElement().breaks().add((IBranch) builder.insertInstruction(
                    condEndBlock,
                    new IBranch(cond, bodyBlock, null)
                ));
                visitStmt(forStmt.getBody());

                // update
                BasicBlock updateBlock = builder.createBlock(true, "For Update Block");
                Optional.ofNullable(forStmt.getUpdate()).ifPresent(this::visitForStmt);
                BasicBlock updateEndBlock = builder.getInsertPoint();
                builder.insertInstruction(
                    new IBranch(condBlock)
                );

                // after
                BasicBlock exitBlock = builder.createBlock(false, "For Exit Block");

                // replace the targets
                localLoops.lastElement().replaceBreak(exitBlock);
                localLoops.lastElement().replaceContinue(updateEndBlock);
                localLoops.pop();
            }
            case Break -> localLoops.lastElement().breaks().add((IBranch) builder.insertInstruction(
                new IBranch(null)
            ));
            case Continue -> localLoops.lastElement().continues().add((IBranch) builder.insertInstruction(
                new IBranch(null)
            ));
            case Return -> {
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
                Iterator<Value> args = ((StmtPrintf) stmt).getArgs().stream().map(this::visitExpr).iterator();
                String format = ((StmtPrintf) stmt).getFormat();
                for (int i = 0; i < format.length(); i++) {
                    if (format.charAt(i) == '%' && i < format.length() - 1 && "dc".indexOf(format.charAt(i + 1)) >= 0) {
                        String function = format.charAt(i + 1) == 'd' ? "putint" : "putch";
                        builder.insertInstruction(
                            new ICall(builder.getExternalFunction(function), List.of(
                                Compatibility.unityIntoInteger(args.next())
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
        if (left.getIndices().isEmpty()) {
            builder.insertInstruction(
                new IStore(right, left.getSymbol().getValue())
            );
        } else {
            builder.insertInstruction(
                new IStore(right, builder.getGep(
                    left.getSymbol(), left.getIndices().stream().map(this::visitExpr).toList()
                ))
            );
        }
    }
}
