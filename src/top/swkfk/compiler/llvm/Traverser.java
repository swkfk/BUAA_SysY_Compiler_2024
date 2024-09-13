package top.swkfk.compiler.llvm;

import top.swkfk.compiler.Controller;
import top.swkfk.compiler.frontend.ast.CompileUnit;
import top.swkfk.compiler.frontend.ast.block.Block;
import top.swkfk.compiler.frontend.ast.block.BlockItem;
import top.swkfk.compiler.frontend.ast.declaration.function.FuncDef;
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
import top.swkfk.compiler.frontend.ast.misc.LeftValue;
import top.swkfk.compiler.frontend.ast.misc.Number;
import top.swkfk.compiler.frontend.ast.statement.Stmt;
import top.swkfk.compiler.frontend.symbol.SymbolTable;
import top.swkfk.compiler.frontend.symbol.type.Ty;
import top.swkfk.compiler.frontend.symbol.type.TyPtr;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.llvm.value.constants.ConstInteger;
import top.swkfk.compiler.llvm.value.instruction.BinaryOp;
import top.swkfk.compiler.llvm.value.instruction.IAllocate;
import top.swkfk.compiler.llvm.value.instruction.IBinary;
import top.swkfk.compiler.llvm.value.instruction.ICall;
import top.swkfk.compiler.llvm.value.instruction.ILoad;
import top.swkfk.compiler.llvm.value.instruction.IStore;

import java.util.List;

class Traverser {
    private final CompileUnit ast;
    private final IrBuilder builder;
    private final SymbolTable symbols = Controller.symbols;

    Traverser(CompileUnit ast, IrBuilder builder) {
        this.ast = ast;
        this.builder = builder;
    }

    List<Decl> getGlobalVariables() {
        return ast.getDeclarations();
    }

    List<FuncDef> getFunctions() {
        return ast.getFunctions();
    }

    void visitFunction(FuncDef funcDef) {
        builder.registerFunction(
            funcDef.getSymbol().getName(), funcDef.getSymbol().getType(), funcDef.getParams()
        );
        builder.jumpToNewBlock();
        visitBlock(funcDef.getBody());
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
                case ADD -> now = builder.insertInstruction(new IBinary(BinaryOp.ADD, now, right));
                case SUB -> now = builder.insertInstruction(new IBinary(BinaryOp.SUB, now, right));
            }
        }
        return now;
    }

    Value visitMulExpr(ExprMul expr) {
        Value now = visitUnaryExpr(expr.getLeft());
        for (int i = 0; i < expr.getOps().size(); i++) {
            Value right = visitUnaryExpr(expr.getRights().get(i));
            switch (expr.getOps().get(i)) {
                case MUL -> now = builder.insertInstruction(new IBinary(BinaryOp.MUL, now, right));
                case DIV -> now = builder.insertInstruction(new IBinary(BinaryOp.DIV, now, right));
                case MOD -> now = builder.insertInstruction(new IBinary(BinaryOp.MOD, now, right));
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
                    case Minus -> builder.insertInstruction(new IBinary(BinaryOp.SUB, ConstInteger.zero, value));
                    case Not -> builder.insertInstruction(new IBinary(BinaryOp.XOR, ConstInteger.logicOne, value));
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
            case LVal -> {
                LeftValue lVal = (LeftValue) expr.getValue();
                if (lVal.getIndices().isEmpty()) {
                    yield builder.insertInstruction(
                        new ILoad(lVal.getSymbol().getValue())
                    );
                } else {
                    yield builder.getGep(
                        lVal.getSymbol(), lVal.getIndices().stream().map(this::visitExpr).toList()
                    );
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
                case Lt -> ret = builder.insertInstruction(new IBinary(BinaryOp.Lt, ret, right));
                case Gt -> ret = builder.insertInstruction(new IBinary(BinaryOp.Gt, ret, right));
                case Le -> ret = builder.insertInstruction(new IBinary(BinaryOp.Le, ret, right));
                case Ge -> ret = builder.insertInstruction(new IBinary(BinaryOp.Ge, ret, right));
            }
        }
        return ret;
    }

    Value visitCondEqu(CondEqu equ) {
        Value ret = visitCondRel(equ.getLeft());
        for (int i = 0; i < equ.getOps().size(); i++) {
            Value right = visitCondRel(equ.getRights().get(i));
            switch (equ.getOps().get(i)) {
                case Eq -> ret = builder.insertInstruction(new IBinary(BinaryOp.Eq, ret, right));
                case Ne -> ret = builder.insertInstruction(new IBinary(BinaryOp.Ne, ret, right));
            }
        }
        return ret;
    }

    Value visitCondAnd(CondAnd and) {
        return and.getCondEquList().stream().map(this::visitCondEqu).reduce(
            (a, b) -> builder.insertInstruction(new IBinary(BinaryOp.AND, a, b))
        ).orElse(ConstInteger.logicOne);
    }

    Value visitCondOr(CondOr or) {
        return or.getCondAndList().stream().map(this::visitCondAnd).reduce(
            (a, b) -> builder.insertInstruction(new IBinary(BinaryOp.OR, a, b))
        ).orElse(ConstInteger.logicZero);
    }

    Value visitCond(Cond cond) {
        return visitCondOr(cond.getCondOr());
    }

    void visitStmt(Stmt stmt) {

    }

}
