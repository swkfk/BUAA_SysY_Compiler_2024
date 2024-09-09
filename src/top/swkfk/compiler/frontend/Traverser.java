package top.swkfk.compiler.frontend;

import top.swkfk.compiler.error.ErrorTable;
import top.swkfk.compiler.frontend.ast.CompileUnit;
import top.swkfk.compiler.frontend.ast.block.Block;
import top.swkfk.compiler.frontend.ast.block.BlockItem;
import top.swkfk.compiler.frontend.ast.declaration.function.FuncDef;
import top.swkfk.compiler.frontend.ast.declaration.function.FuncFormalParam;
import top.swkfk.compiler.frontend.ast.declaration.function.FuncType;
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
import top.swkfk.compiler.frontend.ast.logical.CondRel;
import top.swkfk.compiler.frontend.ast.misc.ForStmt;
import top.swkfk.compiler.frontend.ast.misc.LeftValue;
import top.swkfk.compiler.frontend.ast.statement.Stmt;
import top.swkfk.compiler.frontend.ast.statement.StmtAssign;
import top.swkfk.compiler.frontend.ast.statement.StmtBlock;
import top.swkfk.compiler.frontend.ast.statement.StmtExpr;
import top.swkfk.compiler.frontend.ast.statement.StmtFor;
import top.swkfk.compiler.frontend.ast.statement.StmtGetInt;
import top.swkfk.compiler.frontend.ast.statement.StmtIf;
import top.swkfk.compiler.frontend.ast.statement.StmtPrintf;
import top.swkfk.compiler.frontend.ast.statement.StmtReturn;
import top.swkfk.compiler.frontend.symbol.Symbol;
import top.swkfk.compiler.frontend.symbol.SymbolFunction;
import top.swkfk.compiler.frontend.symbol.SymbolTable;

import java.util.LinkedList;
import java.util.Optional;

/**
 * A simple traverser to build the symbol table.
 */
public class Traverser {
    private final CompileUnit ast;
    private final ErrorTable errors;
    private final SymbolTable symbols;

    public Traverser(CompileUnit ast, ErrorTable errors, SymbolTable symbols) {
        this.ast = ast;
        this.errors = errors;
        this.symbols = symbols;
    }

    public void spawn() {
        ast.getDeclarations().forEach(this::registerDecl);
        for (FuncDef func : ast.getFunctions()) {
            SymbolFunction symbol = symbols.addFunction(
                func.getIdentifier(),
                func.getType().is(FuncType.Type.Void) ? Symbol.Type.Void : Symbol.Type.Int
            );
            func.setSymbol(symbol);

            symbols.newScope();
            for (FuncFormalParam param : func.getParams()) {
                // TODO: Now ignore the indices
                param.setSymbol(symbols.addParameter(symbol, param.getIdentifier(), Symbol.Type.Int));
            }
            visitBlock(func.getBody());
            symbols.exitScope();
        }
        symbols.newScope();
        visitBlock(ast.getMainFunc().getBlock());
        symbols.exitScope();
    }

    private void registerDecl(Decl decl) {
        if (decl.getType().equals(Decl.Type.Const)) {
            ConstDecl constDecl = (ConstDecl) decl.getDeclaration();
            for (ConstDef def : constDecl.getDefs()) {
                // TODO: Now ignore the indices
                // TODO: Now ignore the const
                def.setSymbol(symbols.addVariable(def.getIdentifier(), Symbol.Type.Int));
            }
        } else {
            VarDecl varDecl = (VarDecl) decl.getDeclaration();
            for (VarDef def : varDecl.getDefs()) {
                // TODO: Now ignore the indices
                def.setSymbol(symbols.addVariable(def.getIdentifier(), Symbol.Type.Int));
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
                Optional.ofNullable(((StmtFor) stmt).getInit()).ifPresent(this::visitForStmt);
                Optional.ofNullable(((StmtFor) stmt).getCondition()).ifPresent(this::visitCond);
                Optional.ofNullable(((StmtFor) stmt).getUpdate()).ifPresent(this::visitForStmt);
                visitNewScopeStmt(((StmtFor) stmt).getBody());
            }
            case GetInt -> visitLeftValue(((StmtGetInt) stmt).getLeft());
            case Printf -> ((StmtPrintf) stmt).getArgs().forEach(this::visitExpr);
            case Return -> Optional.ofNullable(((StmtReturn) stmt).getExpr()).ifPresent(this::visitExpr);
            case Expr -> Optional.ofNullable(((StmtExpr) stmt).getExpr()).ifPresent(this::visitExpr);
            case Assign -> {
                visitLeftValue(((StmtAssign) stmt).getLeft());
                visitExpr(((StmtAssign) stmt).getRight());
            }
            case Block -> visitBlock(((StmtBlock) stmt).getBlock());
            case Break, Continue -> {}
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
        leftValue.setSymbol(symbols.getVariable(leftValue.getIdentifier()));
    }

    private void visitExpr(Expr expr) {
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
                call.setSymbol(symbols.getFunction(call.getIdentifier()));
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
