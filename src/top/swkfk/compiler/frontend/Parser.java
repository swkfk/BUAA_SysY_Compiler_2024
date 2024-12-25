package top.swkfk.compiler.frontend;

import top.swkfk.compiler.Controller;
import top.swkfk.compiler.error.ErrorTable;
import top.swkfk.compiler.error.ErrorType;
import top.swkfk.compiler.frontend.ast.CompileUnit;
import top.swkfk.compiler.frontend.ast.FirstSet;
import top.swkfk.compiler.frontend.ast.block.Block;
import top.swkfk.compiler.frontend.ast.block.BlockItem;
import top.swkfk.compiler.frontend.ast.declaration.BasicType;
import top.swkfk.compiler.frontend.ast.declaration.function.FuncDef;
import top.swkfk.compiler.frontend.ast.declaration.function.FuncFormalParam;
import top.swkfk.compiler.frontend.ast.declaration.function.FuncFormalParams;
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
import top.swkfk.compiler.frontend.ast.logical.CondOr;
import top.swkfk.compiler.frontend.ast.logical.CondRel;
import top.swkfk.compiler.frontend.ast.misc.Char;
import top.swkfk.compiler.frontend.ast.misc.ForStmt;
import top.swkfk.compiler.frontend.ast.misc.FuncRealParams;
import top.swkfk.compiler.frontend.ast.misc.LeftValue;
import top.swkfk.compiler.frontend.ast.misc.Number;
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
import top.swkfk.compiler.frontend.token.Token;
import top.swkfk.compiler.frontend.token.TokenStream;
import top.swkfk.compiler.frontend.token.TokenType;
import top.swkfk.compiler.helpers.ParserWatcher;
import top.swkfk.compiler.utils.BackTrace;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

final public class Parser {
    private final ErrorTable errors = Controller.errors;
    private final CompileUnit ast;

    /* =-=-=-=-=-= <tokens-wrapper> =-=-=-=-=-= */
    // The following methods are wrappers of <code>TokenStream</code> methods.
    // We need to record the tokens with its AST for homework 3.
    // So we need to wrap these methods to record the tokens.

    /**
     * SHALL NOT consume the token without recording via methods of <code>Parser</code>.
     */
    private final TokenStream __tokens;
    private final ParserWatcher __watcher;

    /**
     * Consume the token. Prepared for recording.
     *
     * @param type The specified token type.
     * @return The consumed token.
     */
    private Token consume(TokenType type) {
        Token token = null;
        try {
            token = __tokens.consume(type);
            __watcher.add(token.toString());
        } catch (IllegalStateException e) {
            // 这里只检查一部分错误，当使用 consume 方法时，默认只会出现这些，如果出现其他错误，抛出异常，可能有上层处理。
            if (type.equals(TokenType.Semicolon)) {
                errors.add(ErrorType.ExpectedSemicolon, Objects.requireNonNull(__tokens.peek(-1)).location());
            } else if (type.equals(TokenType.RParen)) {
                errors.add(ErrorType.ExpectedRParen, Objects.requireNonNull(__tokens.peek(-1)).location());
            } else if (type.equals(TokenType.RBracket)) {
                errors.add(ErrorType.ExpectedRBracket, Objects.requireNonNull(__tokens.peek(-1)).location());
            } else {
                throw e;
            }
        }
        return token;
    }

    /**
     * Check and consume the token if it is among the given types. Prepared for recording.
     *
     * @param types The specified token types.
     * @return Whether the token is among the given types.
     */
    private boolean checkConsume(TokenType... types) {
        Token token = __tokens.checkConsume(types);
        if (token != null) {
            __watcher.add(token.toString());
            return true;
        }
        return false;
    }

    /**
     * Get the next token. Prepared for recording.
     *
     * @return The next token.
     */
    private Token next() {
        Token token = __tokens.next();
        __watcher.add(String.valueOf(token));
        return token;
    }

    /**
     * Check whether the next token is among the given types.
     * @param types The specified token types.
     * @return Whether the next token is among the given types.
     * @see Parser#among(int, TokenType...)
     */
    private boolean among(TokenType... types) {
        return among(0, types);
    }

    /**
     * Check whether the next-th token is among the given types.
     * @param next The next-th token.
     * @param types The specified token types.
     * @return The check result.
     */
    private boolean among(int next, TokenType... types) {
        Token tk = __tokens.peek(next);
        if (tk == null) {
            return false;
        }
        return tk.among(types);
    }

    private TokenType peekType() {
        return __tokens.peek().type();
    }

    private boolean eof() {
        return __tokens.eof();
    }

    private<T> T watch(T inst) {
        __watcher.add(inst.toString());
        return inst;
    }

    /* =-=-=-=-=-= </tokens-wrapper> =-=-=-=-=-= */

    /**
     * The check point for backtracking. Use it to try-parse and recover from errors.
     */
    private final BackTrace trace;

    public Parser(TokenStream tokens, ParserWatcher watcher) {
        this.__tokens = tokens;
        this.__watcher = watcher;
        this.ast = new CompileUnit();
        this.trace = new BackTrace(errors, __tokens, __watcher);
    }

    public Parser parse() {
        while (!eof()) {
            if (among(1, TokenType.SpMain)) {
                // Main Function
                ast.setMainFunc(parseMainFuncDef());
            } else if (among(2, TokenType.LParen)) {
                // Function
                ast.addFunction(parseFuncDef());
            } else {
                // Declaration
                ast.addDeclaration(parseDeclaration());
            }
        }
        watch("<CompUnit>");
        return this;
    }

    private MainFuncDef parseMainFuncDef() {
        consume(TokenType.Int);
        Token identifier = consume(TokenType.SpMain);
        consume(TokenType.LParen);
        consume(TokenType.RParen);
        return watch(new MainFuncDef(identifier, parseBlock()));
    }

    private FuncType parseFuncType() {
        Token type = next();
        assert type.among(TokenType.Int, TokenType.Void, TokenType.Char) : "Only support int, char or void type";
        return watch(new FuncType(
            type.is(TokenType.Int) ? FuncType.Type.Int : (
                type.is(TokenType.Char) ? FuncType.Type.Char : FuncType.Type.Void
            )
        ));
    }

    private FuncDef parseFuncDef() {
        FuncType returnType = parseFuncType();
        Token identifier = consume(TokenType.Ident);
        consume(TokenType.LParen);
        // 即使右括号缺失，parseFuncFormalParams 也能处理
        FuncFormalParams params =
            among(TokenType.RParen) ? new FuncFormalParams() : parseFuncFormalParams();
        consume(TokenType.RParen);
        Block body = parseBlock();
        return watch(new FuncDef(returnType, identifier, params, body));
    }

    private FuncFormalParams parseFuncFormalParams() {
        FuncFormalParams params = new FuncFormalParams();
        // 只有以下两种类型
        while (among(TokenType.Int, TokenType.Char)) {
            params.addParam(parseFuncFormalParam());
            checkConsume(TokenType.Comma);
        }
        return watch(params);
    }

    private FuncFormalParam parseFuncFormalParam() {
        Token type = next();
        assert type.among(TokenType.Int, TokenType.Char) : "Only support int or char type";
        BasicType paramType = BasicType.from(type.type());
        Token identifier = consume(TokenType.Ident);
        boolean isArray = among(TokenType.LBracket);
        FuncFormalParam param = new FuncFormalParam(paramType, identifier, isArray);
        if (checkConsume(TokenType.LBracket)) {
            consume(TokenType.RBracket);
            param.addIndex(null);
        }
        while (checkConsume(TokenType.LBracket)) {
            param.addIndex(parseConstExpr());
            consume(TokenType.RBracket);
        }
        return watch(param);
    }

    private Decl parseDeclaration() {
        if (checkConsume(TokenType.Const)) {
            return new Decl(parseConstDeclaration());
        } else {
            return new Decl(parseVarDeclaration());
        }
    }

    private ConstDecl parseConstDeclaration() {
        Token type = next();
        assert type.among(TokenType.Int, TokenType.Char) : "Only support int or char type";
        ConstDecl constDecl = new ConstDecl(BasicType.from(type.type()));
        do {
            constDecl.addDef(parseConstDefinition());
        } while (checkConsume(TokenType.Comma));
        consume(TokenType.Semicolon);
        return watch(constDecl);
    }

    private ConstDef parseConstDefinition() {
        Token identifier = consume(TokenType.Ident);
        List<ExprConst> indices = new LinkedList<>();
        while (checkConsume(TokenType.LBracket)) {
            indices.add(parseConstExpr());
            consume(TokenType.RBracket);
        }
        consume(TokenType.Assign);
        // 各种形式的初始化都会被处理
        ConstInitValue initial = parseConstInitial(false);
        return watch(new ConstDef(identifier, indices, initial));
    }

    private VarDecl parseVarDeclaration() {
        Token type = next();
        assert type.among(TokenType.Int, TokenType.Char) : "Only support int or char type";
        VarDecl varDecl = new VarDecl(BasicType.from(type.type()));
        do {
            varDecl.addDef(parseVarDefinition());
        } while (checkConsume(TokenType.Comma));
        consume(TokenType.Semicolon);
        return watch(varDecl);
    }

    private VarDef parseVarDefinition() {
        VarDef varDef = new VarDef(consume(TokenType.Ident));
        while (checkConsume(TokenType.LBracket)) {
            varDef.addIndex(parseConstExpr());
            consume(TokenType.RBracket);
        }
        if (checkConsume(TokenType.Assign)) {
            varDef.setInitial(parseVarInitial(false));
        }
        return watch(varDef);
    }

    private ExprConst parseConstExpr() {
        return watch(new ExprConst(parseAddExpr()));
    }

    private ConstInitValue parseConstInitial(boolean inSub) {
        if (among(TokenType.StrConst)) {
            return watch(new ConstInitValue(consume(TokenType.StrConst).value()));
        }
        if (!checkConsume(TokenType.LBrace)) {
            if (!inSub) {
                return watch(new ConstInitValue(parseConstExpr()));
            } else {
                return new ConstInitValue(parseConstExpr());
            }
        }
        ConstInitValue constInitValue = new ConstInitValue();
        // 这里是考虑了大括号嵌套的情况，但今年没有出现
        while (!among(TokenType.RBrace)) {
            constInitValue.addSubInitializer(parseConstInitial(true));
            checkConsume(TokenType.Comma);
        }
        consume(TokenType.RBrace);
        return watch(constInitValue);
    }

    private VarInitValue parseVarInitial(boolean inSub) {
        if (among(TokenType.StrConst)) {
            return watch(new VarInitValue(consume(TokenType.StrConst).value()));
        }
        if (!checkConsume(TokenType.LBrace)) {
            if (!inSub) {
                return watch(new VarInitValue(parseExpr()));
            } else {
                return new VarInitValue(parseExpr());
            }
        }
        VarInitValue varInitValue = new VarInitValue();
        while (!among(TokenType.RBrace)) {
            varInitValue.addSubInitializer(parseVarInitial(true));
            checkConsume(TokenType.Comma);
        }
        consume(TokenType.RBrace);
        return watch(varInitValue);
    }

    private Block parseBlock() {
        Block block = new Block();
        consume(TokenType.LBrace);
        while (!among(TokenType.RBrace)) {
            block.addBlock(parseBlockItem());
        }
        // 用于错误处理，记录块的结束位置，右大括号
        block.setEndToken(consume(TokenType.RBrace));
        return watch(block);
    }

    private BlockItem parseBlockItem() {
        if (among(TokenType.Const, TokenType.Int, TokenType.Char)) {
            return new BlockItem(parseDeclaration());
        }
        return new BlockItem(parseStmt());  // Skip watching
    }

    /**
     * 试读一下是否是赋值语句
     * @return 是否是赋值语句
     */
    private boolean tryParseAssign() {
        // 只有左值才可能是赋值语句，通过 First 集判断
        if (!FirstSet.getFirst(LeftValue.class).contains(peekType())) {
            return false;
        }
        // 尝试读取，如果失败，返回 false，并回溯
        try (BackTrace ignore = trace.save()) {
            parseLVal();
            if (!among(TokenType.Assign)) {
                throw new IllegalStateException("Not an assignment");
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private Stmt parseStmt() {
        if (among(TokenType.LBrace)) {
            return watch(new StmtBlock(parseBlock()));
        }
        if (among(TokenType.If)) {
            Token ifTk = consume(TokenType.If);
            consume(TokenType.LParen);
            Cond cond = parseCond();
            consume(TokenType.RParen);
            Stmt thenStmt = parseStmt();
            if (checkConsume(TokenType.Else)) {
                return watch(new StmtIf(cond, thenStmt, parseStmt(), ifTk.location()));
            }
            return watch(new StmtIf(cond, thenStmt, ifTk.location()));
        }
        if (among(TokenType.For)) {
            Token forTk = consume(TokenType.For);
            consume(TokenType.LParen);
            ForStmt forStmt = among(TokenType.Semicolon) ? null : parseForStmt();
            consume(TokenType.Semicolon);
            Cond cond = among(TokenType.Semicolon) ? null : parseCond();
            consume(TokenType.Semicolon);
            ForStmt update = among(TokenType.RParen) ? null : parseForStmt();
            consume(TokenType.RParen);
            return watch(new StmtFor(forStmt, cond, update, parseStmt(), forTk.location()));
        }
        if (among(TokenType.Break)) {
            Token tk = consume(TokenType.Break);
            consume(TokenType.Semicolon);
            return watch(new StmtBreak(tk));
        }
        if (among(TokenType.Continue)) {
            Token tk = consume(TokenType.Continue);
            consume(TokenType.Semicolon);
            return watch(new StmtContinue(tk));
        }
        if (among(TokenType.Return)) {
            Token tk = consume(TokenType.Return);
            if (FirstSet.getFirst(Expr.class).contains(peekType())) {
                Expr expr = parseExpr();
                consume(TokenType.Semicolon);
                return watch(new StmtReturn(expr, tk));
            }
            consume(TokenType.Semicolon);
            return watch(new StmtReturn(tk));
        }
        if (among(TokenType.SpPrintf)) {
            Token tk = consume(TokenType.SpPrintf);
            consume(TokenType.LParen);
            StmtPrintf printf = new StmtPrintf(tk, consume(TokenType.StrConst).value());
            while (checkConsume(TokenType.Comma)) {
                printf.addArg(parseExpr());
            }
            consume(TokenType.RParen);
            consume(TokenType.Semicolon);
            return watch(printf);
        }
        if (tryParseAssign()) {
            // 试读赋值语句，如果是，直接解析
            LeftValue lVal = parseLVal();
            consume(TokenType.Assign);
            if (checkConsume(TokenType.SpGetInt)) {
                consume(TokenType.LParen);
                consume(TokenType.RParen);
                consume(TokenType.Semicolon);
                return watch(new StmtGetInt(lVal));
            }
            if (checkConsume(TokenType.SpGetChar)) {
                consume(TokenType.LParen);
                consume(TokenType.RParen);
                consume(TokenType.Semicolon);
                return watch(new StmtGetChar(lVal));
            }
            Expr expr = parseExpr();
            consume(TokenType.Semicolon);
            return watch(new StmtAssign(lVal, expr));
        }
        // 不是赋值语句，只能是 '[Expr] ;'
        if (among(TokenType.Semicolon)) {
            Stmt stmt = new StmtExpr(null);
            consume(TokenType.Semicolon);
            return watch(stmt);
        }
        Stmt stmt = new StmtExpr(parseExpr());
        consume(TokenType.Semicolon);
        return watch(stmt);
    }

    private Expr parseExpr() {
        return watch(new Expr(parseAddExpr()));
    }

    private ExprAdd parseAddExpr() {
        ExprAdd expr = new ExprAdd(parseMulExpr());
        while (among(TokenType.Plus, TokenType.Minus)) {
            watch(expr); // Pay attention to the order of watching
            Token token = next();
            ExprAdd.Op op = token.is(TokenType.Plus) ? ExprAdd.Op.ADD : ExprAdd.Op.SUB;
            expr.add(op, parseMulExpr());
        }
        return watch(expr);
    }

    private ExprMul parseMulExpr() {
        ExprMul expr = new ExprMul(parseUnaryExpr());
        while (among(TokenType.Mult, TokenType.Div, TokenType.Mod)) {
            watch(expr); // Pay attention to the order of watching
            Token token = next();
            ExprMul.Op op =
                token.is(TokenType.Mult) ? ExprMul.Op.MUL :
                    token.is(TokenType.Div) ? ExprMul.Op.DIV :
                        ExprMul.Op.MOD;
            expr.add(op, parseUnaryExpr());
        }
        return watch(expr);
    }

    private ExprUnaryUnary.Op parseUnaryOp() {
        Token token = next();
        return watch(
            token.is(TokenType.Plus) ? ExprUnaryUnary.Op.Plus :
                token.is(TokenType.Minus) ? ExprUnaryUnary.Op.Minus :
                    ExprUnaryUnary.Op.Not
        );
    }

    private FuncRealParams parseFuncRealParams() {
        FuncRealParams params = new FuncRealParams();
        do {
            // Pay attention, this is just for passing the test
            // 缺少右括号的情况，后面不能有东西了，否则无法界定，因此判断是否是右括号或分号
            if (among(TokenType.RParen) || among(TokenType.Semicolon)) {
                break;
            }
            params.addParam(parseExpr());
        } while (checkConsume(TokenType.Comma));
        return watch(params);
    }

    private ExprUnary parseUnaryExpr() {
        if (among(TokenType.Plus, TokenType.Minus, TokenType.Not)) {
            return watch(new ExprUnaryUnary(parseUnaryOp(), parseUnaryExpr()));
        }
        if (among(TokenType.Ident) && among(1, TokenType.LParen)) {
            // 函数调用
            Token identifier = consume(TokenType.Ident);
            consume(TokenType.LParen);
            ExprUnaryCall call;
            if (among(TokenType.RParen)) {
                call = new ExprUnaryCall(identifier, new FuncRealParams());
            } else {
                call = new ExprUnaryCall(identifier, parseFuncRealParams());
            }
            consume(TokenType.RParen);
            return watch(call);
        }
        return watch(new ExprUnaryPrimary(parsePrimaryExpr()));
    }

    private Char parseCharacter() {
        return watch(new Char(next().value().charAt(0)));
    }

    private ExprPrimary parsePrimaryExpr() {
        if (checkConsume(TokenType.LParen)) {
            Expr expr = parseExpr();
            consume(TokenType.RParen);
            return watch(new ExprPrimary(expr));
        }
        if (among(TokenType.IntConst)) {
            return watch(new ExprPrimary(parseNumber()));
        }
        if (among(TokenType.CharConst)) {
            return watch(new ExprPrimary(parseCharacter()));
        }
        return watch(new ExprPrimary(parseLVal()));
    }

    private Number parseNumber() {
        return watch(new Number(Integer.parseInt(next().value())));
    }

    private Cond parseCond() {
        return watch(new Cond(parseLOrExpr()));
    }

    private CondOr parseLOrExpr() {
        CondOr cond = new CondOr();
        cond.addCondAnd(parseLAndExpr());
        while (among(TokenType.Or)) {
            watch(cond); // Pay attention to the order of watching
            consume(TokenType.Or);
            cond.addCondAnd(parseLAndExpr());
        }
        return watch(cond);
    }

    private CondAnd parseLAndExpr() {
        CondAnd cond = new CondAnd();
        cond.addCondEqu(parseEqExpr());
        while (among(TokenType.And)) {
            watch(cond); // Pay attention to the order of watching
            consume(TokenType.And);
            cond.addCondEqu(parseEqExpr());
        }
        return watch(cond);
    }

    private CondEqu parseEqExpr() {
        CondEqu cond = new CondEqu(parseRelExpr());
        while (among(TokenType.Eq, TokenType.Neq)) {
            watch(cond); // Pay attention to the order of watching
            Token token = next();
            CondEqu.Op op = token.is(TokenType.Eq) ? CondEqu.Op.Eq : CondEqu.Op.Ne;
            cond.add(op, parseRelExpr());
        }
        return watch(cond);
    }

    private CondRel parseRelExpr() {
        CondRel cond = new CondRel(parseAddExpr());
        while (among(TokenType.Lt, TokenType.Gt, TokenType.Leq, TokenType.Geq)) {
            watch(cond); // Pay attention to the order of watching
            Token token = next();
            CondRel.Op op =
                token.is(TokenType.Lt) ? CondRel.Op.Lt :
                    token.is(TokenType.Gt) ? CondRel.Op.Gt :
                        token.is(TokenType.Leq) ? CondRel.Op.Le :
                            CondRel.Op.Ge;
            cond.add(op, parseAddExpr());
        }
        return watch(cond);
    }

    private LeftValue parseLVal() {
        LeftValue leftValue = new LeftValue(consume(TokenType.Ident));
        while (checkConsume(TokenType.LBracket)) {
            leftValue.addIndex(parseExpr());
            consume(TokenType.RBracket);
        }
        return watch(leftValue);
    }

    private ForStmt parseForStmt() {
        LeftValue lVal = parseLVal();
        consume(TokenType.Assign);
        Expr expr = parseExpr();
        return watch(new ForStmt(lVal, expr));
    }

    public CompileUnit emit() {
        return ast;
    }
}
