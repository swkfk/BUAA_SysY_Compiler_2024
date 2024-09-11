package top.swkfk.compiler.frontend;

import top.swkfk.compiler.Controller;
import top.swkfk.compiler.error.ErrorTable;
import top.swkfk.compiler.error.ErrorType;
import top.swkfk.compiler.frontend.ast.CompileUnit;
import top.swkfk.compiler.frontend.ast.block.Block;
import top.swkfk.compiler.frontend.ast.block.BlockItem;
import top.swkfk.compiler.frontend.ast.declaration.function.FuncDef;
import top.swkfk.compiler.frontend.ast.declaration.function.FuncFormalParam;
import top.swkfk.compiler.frontend.ast.declaration.function.FuncType;
import top.swkfk.compiler.frontend.ast.declaration.object.ConstDecl;
import top.swkfk.compiler.frontend.ast.declaration.object.ConstDef;
import top.swkfk.compiler.frontend.ast.declaration.object.ConstInitValue;
import top.swkfk.compiler.frontend.ast.declaration.object.Decl;
import top.swkfk.compiler.frontend.ast.declaration.object.VarDecl;
import top.swkfk.compiler.frontend.ast.declaration.object.VarDef;
import top.swkfk.compiler.frontend.ast.declaration.object.VarInitValue;
import top.swkfk.compiler.frontend.ast.expression.Expr;
import top.swkfk.compiler.frontend.ast.expression.ExprAdd;
import top.swkfk.compiler.frontend.ast.expression.ExprConst;
import top.swkfk.compiler.frontend.ast.expression.ExprMul;
import top.swkfk.compiler.frontend.ast.expression.ExprPrimary;
import top.swkfk.compiler.frontend.ast.expression.ExprUnary;
import top.swkfk.compiler.frontend.ast.expression.ExprUnaryCall;
import top.swkfk.compiler.frontend.ast.expression.ExprUnaryPrimary;
import top.swkfk.compiler.frontend.ast.expression.ExprUnaryUnary;
import top.swkfk.compiler.frontend.ast.logical.Cond;
import top.swkfk.compiler.frontend.ast.logical.CondAnd;
import top.swkfk.compiler.frontend.ast.logical.CondEqu;
import top.swkfk.compiler.frontend.ast.logical.CondRel;
import top.swkfk.compiler.frontend.ast.misc.ForStmt;
import top.swkfk.compiler.frontend.ast.misc.LeftValue;
import top.swkfk.compiler.frontend.ast.statement.Stmt;
import top.swkfk.compiler.frontend.ast.statement.StmtAssign;
import top.swkfk.compiler.frontend.ast.statement.StmtBlock;
import top.swkfk.compiler.frontend.ast.statement.StmtBreak;
import top.swkfk.compiler.frontend.ast.statement.StmtContinue;
import top.swkfk.compiler.frontend.ast.statement.StmtExpr;
import top.swkfk.compiler.frontend.ast.statement.StmtFor;
import top.swkfk.compiler.frontend.ast.statement.StmtGetInt;
import top.swkfk.compiler.frontend.ast.statement.StmtIf;
import top.swkfk.compiler.frontend.ast.statement.StmtPrintf;
import top.swkfk.compiler.frontend.ast.statement.StmtReturn;
import top.swkfk.compiler.frontend.symbol.FixedArray;
import top.swkfk.compiler.frontend.symbol.FixedValue;
import top.swkfk.compiler.frontend.symbol.SymbolFunction;
import top.swkfk.compiler.frontend.symbol.SymbolTable;
import top.swkfk.compiler.frontend.symbol.SymbolVariable;
import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.frontend.symbol.type.Ty;
import top.swkfk.compiler.frontend.symbol.type.TyArray;
import top.swkfk.compiler.frontend.symbol.type.TyPtr;

import java.util.LinkedList;
import java.util.Optional;

/**
 * A simple traverser to build the symbol table.
 */
public class Traverser {
    private final CompileUnit ast;
    private final ErrorTable errors = Controller.errors;
    private final SymbolTable symbols = Controller.symbols;

    private int loopDepth = 0;
    private boolean returnIntFound = false;
    private boolean notAllowIntReturn = false;

    public Traverser(CompileUnit ast) {
        this.ast = ast;
    }

    public void spawn() {
        ast.getDeclarations().forEach(this::registerDecl);
        for (FuncDef func : ast.getFunctions()) {
            SymbolFunction symbolFunc = symbols.addFunction( func.getIdentifier().value(), func.getType());
            if (symbolFunc == null) {
                errors.add(ErrorType.DuplicatedDeclaration, func.getIdentifier().location());
                continue;
            }
            func.setSymbol(symbolFunc);

            symbols.newScope();
            for (FuncFormalParam param : func.getParams()) {
                SymbolVariable symbolParam = symbols.addParameter(
                    symbolFunc, param.getIdentifier().value(), TyArray.from(param.getIndices())
                );
                if (symbolParam == null) {
                    errors.add(ErrorType.DuplicatedDeclaration, param.getIdentifier().location());
                } else {
                    param.setSymbol(symbolParam);
                }
            }
            returnIntFound = false;
            notAllowIntReturn = !func.getType().is(FuncType.Type.Int);
            visitBlock(func.getBody());
            if (!returnIntFound && !func.getType().is(FuncType.Type.Void)) {
                errors.add(ErrorType.MissingReturnStatement, func.getBody().getEndToken().location());
            }
            symbols.exitScope();
        }
        symbols.newScope();
        returnIntFound = false;
        notAllowIntReturn = false;
        visitBlock(ast.getMainFunc().getBlock());
        if (!returnIntFound) {
            errors.add(ErrorType.MissingReturnStatement, ast.getMainFunc().getBlock().getEndToken().location());
        }
        symbols.exitScope();
    }

    private void registerDecl(Decl decl) {
        if (decl.getType().equals(Decl.Type.Const)) {
            ConstDecl constDecl = (ConstDecl) decl.getDeclaration();
            for (ConstDef def : constDecl.getDefs()) {
                def.getIndices().forEach(this::visitExprConst);
                SymbolType ty = TyArray.from(def.getIndices());
                ty.setConst();
                SymbolVariable symbol = symbols.addVariable(def.getIdentifier().value(), ty);
                if (symbol == null) {
                    errors.add(ErrorType.DuplicatedDeclaration, def.getIdentifier().location());
                } else {
                    visitConstInitValue(def.getInitial());
                    try {
                        if (def.getIndices().isEmpty()) {
                            symbol.setConstantValue(new FixedValue(def.getInitial().getExpr().calculate()));
                        } else {
                            symbol.setConstantValue(FixedArray.from(((TyArray) ty).getIndices(), def.getInitial()));
                        }
                    } catch (Exception e) {
                        // Ignore the error, it will be reported by the error handler
                    }
                    def.setSymbol(symbol);
                }
            }
        } else {
            VarDecl varDecl = (VarDecl) decl.getDeclaration();
            for (VarDef def : varDecl.getDefs()) {
                def.getIndices().forEach(this::visitExprConst);
                SymbolVariable symbol = symbols.addVariable(def.getIdentifier().value(), TyArray.from(def.getIndices()));
                if (symbol == null) {
                    errors.add(ErrorType.DuplicatedDeclaration, def.getIdentifier().location());
                } else {
                    visitVarInitValue(def.getInitial());
                    def.setSymbol(symbol);
                }
            }
        }
    }

    private void visitBlock(Block block) {
        for (BlockItem item : block.getItems()) {
            visitBlockItem(item);
        }
    }

    private void visitBlockItem(BlockItem item) {
        if (item.getType().equals(BlockItem.Type.Decl)) {
            registerDecl((Decl) item.getItem());
        } else {
            visitStmt((Stmt) item.getItem());
        }
    }

    private void visitNewScopeStmt(Stmt stmt) {
        symbols.newScope();
        visitStmt(stmt);
        symbols.exitScope();
    }

    private void visitStmt(Stmt stmt) {
        switch (stmt.getType()) {
            case If -> {
                visitCond(((StmtIf) stmt).getCondition());
                visitNewScopeStmt(((StmtIf) stmt).getThenStmt());
                Optional.ofNullable(((StmtIf) stmt).getElseStmt()).ifPresent(this::visitNewScopeStmt);
            }
            case For -> {
                loopDepth++;
                Optional.ofNullable(((StmtFor) stmt).getInit()).ifPresent(this::visitForStmt);
                Optional.ofNullable(((StmtFor) stmt).getCondition()).ifPresent(this::visitCond);
                Optional.ofNullable(((StmtFor) stmt).getUpdate()).ifPresent(this::visitForStmt);
                visitNewScopeStmt(((StmtFor) stmt).getBody());
                loopDepth--;
            }
            case GetInt -> visitLeftValue(((StmtGetInt) stmt).getLeft());
            case Printf -> {
                if (((StmtPrintf) stmt).getArgs().size() != ((StmtPrintf) stmt).getFormatArgCount()) {
                    errors.add(ErrorType.MismatchedFormatArgument, ((StmtPrintf) stmt).getToken().location());
                }
                ((StmtPrintf) stmt).getArgs().forEach(this::visitExpr);
            }
            case Return -> {
                if (((StmtReturn) stmt).getExpr() != null) {
                    returnIntFound = true;
                    if (notAllowIntReturn) {
                        errors.add(ErrorType.MismatchedReturnType, ((StmtReturn) stmt).getToken().location());
                    }
                    visitExpr(((StmtReturn) stmt).getExpr());
                }
            }
            case Expr -> Optional.ofNullable(((StmtExpr) stmt).getExpr()).ifPresent(this::visitExpr);
            case Assign -> {
                visitLeftValue(((StmtAssign) stmt).getLeft());
                Optional.ofNullable(((StmtAssign) stmt).getLeft().getSymbol()).ifPresent(
                    symbol -> {
                        if (symbol.getType().isConst()) {
                            errors.add(ErrorType.AssignToConstant, ((StmtAssign) stmt).getLeft().getIdentifier().location());
                        }
                    }
                );
                visitExpr(((StmtAssign) stmt).getRight());
            }
            case Block -> visitBlock(((StmtBlock) stmt).getBlock());
            case Break -> {
                if (loopDepth == 0) {
                    errors.add(ErrorType.InvalidLoopControl, ((StmtBreak) stmt).getToken().location());
                }
            }
            case Continue -> {
                if (loopDepth == 0) {
                    errors.add(ErrorType.InvalidLoopControl, ((StmtContinue) stmt).getToken().location());
                }
            }
        }
    }

    private void visitForStmt(ForStmt forStmt) {
        visitLeftValue(forStmt.getLeft());
        visitExpr(forStmt.getRight());
    }

    private void visitCond(Cond cond) {
        for (CondAnd and : cond.getCondOr().getCondAndList()) {
            for (CondEqu equ : and.getCondEquList()) {
                for (CondRel rel : new LinkedList<CondRel>() {{
                    add(equ.getLeft());
                    addAll(equ.getRights());
                }}) {
                    visitAddExpr(rel.getLeft());
                    rel.getRights().forEach(this::visitAddExpr);
                }
            }
        }
    }

    private void visitLeftValue(LeftValue leftValue) {
        SymbolVariable symbol = symbols.getVariable(leftValue.getIdentifier().value());
        if (symbol == null) {
            errors.add(ErrorType.UndefinedReference, leftValue.getIdentifier().location());
        } else {
            leftValue.setSymbol(symbol);
            leftValue.getIndices().forEach(this::visitExpr);
        }
    }

    private void visitExpr(Expr expr) {
        visitAddExpr(expr.getExpr());
    }

    private void visitVarInitValue(VarInitValue init) {
        if (init == null) {
            return;
        }
        if (init.getExpr() == null) {
            init.getSubInitializers().forEach(this::visitVarInitValue);
        } else {
            visitExpr(init.getExpr());
        }
    }

    private void visitConstInitValue(ConstInitValue init) {
        if (init.getExpr() == null) {
            init.getSubInitializers().forEach(this::visitConstInitValue);
        } else {
            visitExprConst(init.getExpr());
        }
    }

    private void visitExprConst(ExprConst expr) {
        visitAddExpr(expr.getExpr());
    }

    private void visitAddExpr(ExprAdd expr) {
        for (ExprMul mul : new LinkedList<ExprMul>() {{
            add(expr.getLeft());
            addAll(expr.getRights());
        }}) {
            for (ExprUnary unary : new LinkedList<ExprUnary>() {{
                add(mul.getLeft());
                addAll(mul.getRights());
            }}) {
                visitUnaryExpr(unary);
            }
        }
    }

    private void visitUnaryExpr(ExprUnary unary) {
        switch (unary.getType()) {
            case Unary -> visitUnaryExpr(((ExprUnaryUnary) unary).getExpr());
            case Primary -> visitPrimaryExpr(((ExprUnaryPrimary) unary).getPrimary());
            case Call -> {
                ExprUnaryCall call = (ExprUnaryCall) unary;
                call.getParams().forEach(this::visitExpr);
                call.setSymbol(symbols.getFunction(call.getIdentifier().value()));
                if (call.getSymbol() == null) {
                    errors.add(ErrorType.UndefinedReference, call.getIdentifier().location());
                    return;
                }
                if (call.getParams().size() != call.getSymbol().getParameters().size()) {
                    errors.add(ErrorType.MismatchedParameterCount, call.getIdentifier().location());
                } else {
                    for (int i = 0; i < call.getParams().size(); i++) {
                        SymbolType fType = call.getSymbol().getParameters().get(i).getType();
                        SymbolType rType = call.getParams().get(i).calculateType();
                        if (!fType.equals(rType)) {
                            errors.add(ErrorType.MismatchedParameterType, call.getIdentifier().location());
                        }
                    }
                }
            }
        }
    }

    private void visitPrimaryExpr(ExprPrimary primary) {
        switch (primary.getType()) {
            case Number -> {}
            case LVal -> visitLeftValue((LeftValue) primary.getValue());
            case Expr -> visitExpr((Expr) primary.getValue());
        }
    }
}
