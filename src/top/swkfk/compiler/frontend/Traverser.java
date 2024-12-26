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
import top.swkfk.compiler.frontend.ast.statement.StmtGetChar;
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
import top.swkfk.compiler.frontend.symbol.type.TyArray;

import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * A simple traverser to build the symbol table.
 */
final public class Traverser {
    private final CompileUnit ast;
    private final ErrorTable errors = Controller.errors;
    private final SymbolTable symbols = Controller.symbols;

    // 循环深度，只用来检查 break 和 continue 语句的合法性
    private int loopDepth = 0;
    // 用于检查函数是否出现了带返回值的 return 语句
    private boolean returnIntFound = false;
    // 用于检查函数是否不允许有返回值（void）
    private boolean notAllowIntReturn = false;

    public Traverser(CompileUnit ast) {
        this.ast = ast;
    }

    public void spawn() {
        // 注册所有的全局声明
        ast.getDeclarations().forEach(this::registerDecl);
        // 依次处理每一个函数
        for (FuncDef func : ast.getFunctions()) {
            // 向符号表中添加函数，同时会进行查找
            SymbolFunction symbolFunc = symbols.addFunction(func.getIdentifier().value(), func.getType());
            if (symbolFunc == null) {
                // 如果函数名重复，或与全局变量重名，报错
                errors.add(ErrorType.DuplicatedDeclaration, func.getIdentifier().location());
                continue;
            }
            // 向语法树结构中设置符号
            func.setSymbol(symbolFunc);

            symbols.newScope();
            for (FuncFormalParam param : func.getParams()) {
                // 向符号表中添加参数，同时会进行查找
                SymbolVariable symbolParam = symbols.addParameter(
                    symbolFunc, param.getIdentifier().value(), TyArray.from(param.getType().into(), param.getIndices())
                );
                if (symbolParam == null) {
                    // 如果参数名重复，报错
                    errors.add(ErrorType.DuplicatedDeclaration, param.getIdentifier().location());
                } else {
                    // 向语法树结构中设置符号
                    param.setSymbol(symbolParam);
                }
            }
            // 初始化返回值状态
            returnIntFound = false;
            // 这里注意在新增类型时需要修改！
            notAllowIntReturn = !func.getType().is(FuncType.Type.Int) && !func.getType().is(FuncType.Type.Char);
            // 访问函数体
            visitBlock(func.getBody());
            // 缺少必要的返回值
            if (!returnIntFound && !func.getType().is(FuncType.Type.Void)) {
                errors.add(ErrorType.MissingReturnStatement, func.getBody().getEndToken().location());
            }
            symbols.exitScope();
        }
        // main function
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
        // 处理变量声明，局部的，或者全局的
        // 两个分支几乎一致，只是
        if (decl.getType().equals(Decl.Type.Const)) {
            ConstDecl constDecl = (ConstDecl) decl.getDeclaration();
            for (ConstDef def : constDecl.getDefs()) {
                // 遍历数组索引，访问其中的变量
                def.getIndices().forEach(this::visitExprConst);
                SymbolType ty = TyArray.from(constDecl.getType().into(), def.getIndices());
                // 向符号表中添加变量，同时会进行查找
                SymbolVariable symbol = symbols.addVariable(def.getIdentifier().value(), ty);
                if (symbol == null) {
                    errors.add(ErrorType.DuplicatedDeclaration, def.getIdentifier().location());
                } else {
                    symbol.setConst();
                    // 访问初始化值，每个遍历一遍
                    visitConstInitValue(def.getInitial());
                    try {
                        // 两种不同的初始化值，分别处理，这里会进行常量值的计算！
                        if (def.getIndices().isEmpty()) {
                            // It is impossible that the def.getInitial() is null
                            symbol.setConstantValue(new FixedValue(def.getInitial().getExpr().calculate(), ty));
                        } else {
                            symbol.setConstantValue(FixedArray.from(((TyArray) ty).getIndices(), def.getInitial(), ty.getFinalBaseType()));
                        }
                    } catch (Exception e) {
                        // Ignore the error, it will be reported by the error handler
                        // 这里面的错误，比如变量未定义，在之前已经被捕获了，这里就直接忽略
                    }
                    def.setSymbol(symbol);
                }
            }
        } else {
            VarDecl varDecl = (VarDecl) decl.getDeclaration();
            for (VarDef def : varDecl.getDefs()) {
                def.getIndices().forEach(this::visitExprConst);
                SymbolType ty = TyArray.from(varDecl.getType().into(), def.getIndices());
                SymbolVariable symbol = symbols.addVariable(def.getIdentifier().value(), ty);
                if (symbol == null) {
                    errors.add(ErrorType.DuplicatedDeclaration, def.getIdentifier().location());
                } else {
                    visitVarInitValue(def.getInitial());
                    try {
                        // It is certain that the VarInitValue in global scope shall be a ConstInitValue
                        if (symbol.isGlobal()) {
                            // 全局变量的初始化值必须是常量，会进行计算！
                            if (def.getIndices().isEmpty()) {
                                // 不是数组
                                VarInitValue init = def.getInitial();
                                if (init == null) {
                                    // 缺省初始化值为 0
                                    symbol.setConstantValue(new FixedValue(0, ty));
                                } else {
                                    symbol.setConstantValue(new FixedValue(
                                        init.getExpr().calculateConst(), ty
                                    ));
                                }
                            } else {
                                // 是数组
                                if (def.getInitial() != null) {
                                    symbol.setConstantValue(
                                        FixedArray.from(((TyArray) ty).getIndices(), def.getInitial().into(), ty.getFinalBaseType())
                                    );
                                } else {
                                    // 缺省初始化值为 0
                                    symbol.setConstantValue(
                                        new FixedArray(((TyArray) ty).getIndices(), ty.getFinalBaseType())
                                    );
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Ignore the error, it will be reported by the error handler
                    }
                    def.setSymbol(symbol);
                }
            }
        }
    }

    private void visitBlock(Block block) {
        // 并不在这里新建符号表！
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

    /**
     * Visit the left value in the assignment-like statement. It will check the const-assign error.
     * @param left The left value to visit
     */
    private void visitAssignLikeLeftValue(LeftValue left) {
        visitLeftValue(left);
        Optional.ofNullable(left.getSymbol()).ifPresent(
            symbol -> {
                if (symbol.isConst()) {
                    errors.add(ErrorType.AssignToConstant, left.getIdentifier().location());
                }
            }
        );
    }

    private void visitStmt(Stmt stmt) {
        boolean localReturnIntFound = false;  // Just find the final stmt to find the int return value
        switch (stmt.getType()) {
            case If -> {
                visitCond(((StmtIf) stmt).getCondition());
                visitStmt(((StmtIf) stmt).getThenStmt());
                Optional.ofNullable(((StmtIf) stmt).getElseStmt()).ifPresent(this::visitStmt);
            }
            case For -> {
                loopDepth++;
                Optional.ofNullable(((StmtFor) stmt).getInit()).ifPresent(this::visitForStmt);
                Optional.ofNullable(((StmtFor) stmt).getCondition()).ifPresent(this::visitCond);
                Optional.ofNullable(((StmtFor) stmt).getUpdate()).ifPresent(this::visitForStmt);
                visitStmt(((StmtFor) stmt).getBody());
                loopDepth--;
            }
            case GetInt -> visitAssignLikeLeftValue(((StmtGetInt) stmt).getLeft());
            case GetChar -> visitAssignLikeLeftValue(((StmtGetChar) stmt).getLeft());
            case Printf -> {
                if (((StmtPrintf) stmt).getArgs().size() != ((StmtPrintf) stmt).getFormatArgCount()) {
                    errors.add(ErrorType.MismatchedFormatArgument, ((StmtPrintf) stmt).getToken().location());
                }
                ((StmtPrintf) stmt).getArgs().forEach(this::visitExpr);
            }
            case Return -> {
                if (((StmtReturn) stmt).getExpr() != null) {
                    localReturnIntFound = true;
                    if (notAllowIntReturn) {
                        errors.add(ErrorType.MismatchedReturnType, ((StmtReturn) stmt).getToken().location());
                    }
                    visitExpr(((StmtReturn) stmt).getExpr());
                }
            }
            case Expr ->
                Optional.ofNullable(((StmtExpr) stmt).getExpr()).ifPresent(this::visitExpr);
            case Assign -> {
                visitAssignLikeLeftValue(((StmtAssign) stmt).getLeft());
                visitExpr(((StmtAssign) stmt).getRight());
            }
            case Block -> {
                symbols.newScope();
                visitBlock(((StmtBlock) stmt).getBlock());
                symbols.exitScope();
            }
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
        returnIntFound = localReturnIntFound;
    }

    private void visitForStmt(ForStmt forStmt) {
        visitAssignLikeLeftValue(forStmt.getLeft());
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
        // 这里查找符号表
        SymbolVariable symbol = symbols.getVariable(leftValue.getIdentifier().value());
        if (symbol == null) {
            errors.add(ErrorType.UndefinedReference, leftValue.getIdentifier().location());
        } else {
            // 查到了，就将符号设置到语法树节点中
            leftValue.setSymbol(symbol);
            // 遍历每一个索引！
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
        if (init.getSubInitializers() != null) {
            init.getSubInitializers().forEach(this::visitVarInitValue);
        }
        if (init.getExpr() != null) {
            visitExpr(init.getExpr());
        }
    }

    private void visitConstInitValue(ConstInitValue init) {
        // 根据文法，这里的 init 一定不是 null
        if (init.getSubInitializers() != null) {
            init.getSubInitializers().forEach(this::visitConstInitValue);
        }
        if (init.getExpr() != null) {
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
                // 遍历参数，进行访问
                call.getParams().forEach(this::visitExpr);
                // 查找函数符号
                call.setSymbol(symbols.getFunction(call.getIdentifier().value()));
                if (call.getSymbol() == null) {
                    errors.add(ErrorType.UndefinedReference, call.getIdentifier().location());
                    return;
                }
                // 检查参数个数和类型
                if (call.getParams().size() != call.getSymbol().getParameters().size()) {
                    errors.add(ErrorType.MismatchedParameterCount, call.getIdentifier().location());
                } else {

                    if (IntStream.range(0, call.getParams().size()).anyMatch(
                        i ->
                            !call.getSymbol().getParameters().get(i).getType()
                                // 递归找到最终的类型！
                                .compatible(call.getParams().get(i).calculateType())
                    )) {
                        errors.add(ErrorType.MismatchedParameterType, call.getIdentifier().location());
                    }
                }
            }
        }
    }

    private void visitPrimaryExpr(ExprPrimary primary) {
        switch (primary.getType()) {
            case Number -> {
            }
            case LVal -> visitLeftValue((LeftValue) primary.getValue());
            case Expr -> visitExpr((Expr) primary.getValue());
        }
    }
}
