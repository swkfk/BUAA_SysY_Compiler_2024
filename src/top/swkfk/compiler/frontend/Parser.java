package top.swkfk.compiler.frontend;

import top.swkfk.compiler.frontend.ast.CompileUnit;
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
import top.swkfk.compiler.frontend.ast.misc.ForStmt;
import top.swkfk.compiler.frontend.ast.misc.LeftValue;
import top.swkfk.compiler.frontend.ast.misc.Number;
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
import top.swkfk.compiler.frontend.token.Token;
import top.swkfk.compiler.frontend.token.TokenStream;
import top.swkfk.compiler.frontend.token.TokenType;
import top.swkfk.compiler.utils.ParserWatcher;

import java.util.LinkedList;
import java.util.List;

public class Parser {
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
        Token token = __tokens.consume(type);
        __watcher.add(token.toString());
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
        __watcher.add(token.toString());
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
        return __tokens.peek(next).among(types);
    }

    private boolean eof() {
        return __tokens.eof();
    }

    /* =-=-=-=-=-= </tokens-wrapper> =-=-=-=-=-= */

    public Parser(TokenStream tokens, ParserWatcher watcher) {
        this.__tokens = tokens;
        this.__watcher = watcher;
        this.ast = new CompileUnit();
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
        return this;
    }

    private MainFuncDef parseMainFuncDef() {
        consume(TokenType.Int);
        consume(TokenType.SpMain);
        consume(TokenType.LParen);
        consume(TokenType.RParen);
        return new MainFuncDef(parseBlock());
    }

    private FuncDef parseFuncDef() {
        Token type = next();
        assert type.among(TokenType.Int, TokenType.Void) : "Only support int or void type";
        FuncType returnType = new FuncType(type.is(TokenType.Int) ? FuncType.Type.Int : FuncType.Type.Void);
        String name = consume(TokenType.Ident).value();
        consume(TokenType.LParen);
        FuncFormalParams params = parseFuncFormalParams();
        consume(TokenType.RParen);
        Block body = parseBlock();
        return new FuncDef(returnType, name, params, body);
    }

    private FuncFormalParams parseFuncFormalParams() {
        FuncFormalParams params = new FuncFormalParams();
        while (!among(TokenType.RParen)) {
            params.addParam(parseFuncFormalParam());
            checkConsume(TokenType.Comma);
        }
        return params;
    }

    private FuncFormalParam parseFuncFormalParam() {
        Token type = next();
        assert type.is(TokenType.Int) : "Only support int type";
        BasicType paramType = new BasicType();
        String name = consume(TokenType.Ident).value();
        boolean isArray = among(TokenType.LBracket);
        FuncFormalParam param = new FuncFormalParam(paramType, name, isArray);
        while (checkConsume(TokenType.LBracket)) {
            if (among(TokenType.RBracket)) {
                param.addIndex(null);
            } else {
                param.addIndex(parseConstExpr());
                consume(TokenType.RBracket);
            }
        }
        return param;
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
        assert type.is(TokenType.Int) : "Only support int type";
        ConstDecl constDecl = new ConstDecl(new BasicType());
        do {
            constDecl.addDef(parseConstDefinition());
        } while (checkConsume(TokenType.Comma));
        return constDecl;
    }

    private ConstDef parseConstDefinition() {
        String name = consume(TokenType.Ident).value();
        List<ExprConst> indices = new LinkedList<>();
        while (checkConsume(TokenType.LBracket)) {
            indices.add(parseConstExpr());
            consume(TokenType.RBracket);
        }
        consume(TokenType.Assign);
        ConstInitValue initial = parseConstInitial();
        return new ConstDef(name, indices, initial);
    }

    private VarDecl parseVarDeclaration() {
        Token type = next();
        assert type.is(TokenType.Int) : "Only support int type";
        VarDecl varDecl = new VarDecl(new BasicType());
        do {
            varDecl.addDef(parseVarDefinition());
        } while (checkConsume(TokenType.Comma));
        return varDecl;
    }

    private VarDef parseVarDefinition() {
        VarDef varDef = new VarDef(consume(TokenType.Ident).value());
        while (checkConsume(TokenType.LBracket)) {
            varDef.addIndex(parseConstExpr());
            consume(TokenType.RBracket);
        }
        if (checkConsume(TokenType.Assign)) {
            varDef.setInitial(parseVarInitial());
        }
        return varDef;
    }

    private ExprConst parseConstExpr() {
        return new ExprConst(parseAddExpr());
    }

    private ConstInitValue parseConstInitial() {
        return new ConstInitValue(parseConstExpr());
    }

    private VarInitValue parseVarInitial() {
        if (!checkConsume(TokenType.LBrace)) {
            return new VarInitValue(parseExpr());
        }
        VarInitValue varInitValue = new VarInitValue();
        while (!among(TokenType.RBrace)) {
            varInitValue.addSubInitializer(parseVarInitial());
            checkConsume(TokenType.Comma);
        }
        consume(TokenType.RBrace);
        return varInitValue;
    }

    private Block parseBlock() {
        Block block = new Block();
        consume(TokenType.LBrace);
        while (!checkConsume(TokenType.RBrace)) {
            block.addBlock(parseBlockItem());
        }
        return block;
    }

    private BlockItem parseBlockItem() {
        if (among(TokenType.Const, TokenType.IntConst)) {
            return new BlockItem(parseDeclaration());
        }
        return new BlockItem(parseStmt());
    }

    private Stmt parseStmt() {
        if (among(TokenType.LBrace)) {
            return new StmtBlock(parseBlock());
        }
        if (checkConsume(TokenType.If)) {
            consume(TokenType.LParen);
            Cond cond = parseCond();
            consume(TokenType.RParen);
            Stmt thenStmt = parseStmt();
            if (checkConsume(TokenType.Else)) {
                return new StmtIf(cond, thenStmt, parseStmt());
            }
            return new StmtIf(cond, thenStmt);
        }
        if (checkConsume(TokenType.For)) {
            consume(TokenType.LParen);
            ForStmt forStmt = among(TokenType.Semicolon) ? null : parseForStmt();
            consume(TokenType.Semicolon);
            Cond cond = among(TokenType.Semicolon) ? null : parseCond();
            consume(TokenType.Semicolon);
            ForStmt update = among(TokenType.RParen) ? null : parseForStmt();
            consume(TokenType.RParen);
            return new StmtFor(forStmt, cond, update, parseStmt());
        }
        if (checkConsume(TokenType.Break)) {
            consume(TokenType.Semicolon);
            return new StmtBreak();
        }
        if (checkConsume(TokenType.Continue)) {
            consume(TokenType.Semicolon);
            return new StmtContinue();
        }
        if (checkConsume(TokenType.Return)) {
            if (checkConsume(TokenType.Semicolon)) {
                return new StmtReturn();
            }
            Expr expr = parseExpr();
            consume(TokenType.Semicolon);
            return new StmtReturn(expr);
        }
        if (checkConsume(TokenType.SpPrintf)) {
            consume(TokenType.LParen);
            StmtPrintf printf = new StmtPrintf(consume(TokenType.FString).value());
            while (checkConsume(TokenType.Comma)) {
                printf.addArg(parseExpr());
            }
            consume(TokenType.RParen);
            consume(TokenType.Semicolon);
            return printf;
        }
        int i = 0;
        while (!among(i, TokenType.Assign) && !among(i, TokenType.Semicolon)) {
            i++;
        }
        if (among(i, TokenType.Assign)) {
            LeftValue lVal = parseLVal();
            consume(TokenType.Assign);
            if (checkConsume(TokenType.SpGetInt)) {
                consume(TokenType.LParen);
                consume(TokenType.RParen);
                consume(TokenType.Semicolon);
                return new StmtGetInt(lVal);
            }
            Expr expr = parseExpr();
            consume(TokenType.Semicolon);
            return new StmtAssign(lVal, expr);
        }
        if (checkConsume(TokenType.Semicolon)) {
            return new StmtExpr(null);
        }
        return new StmtExpr(parseExpr());
    }

    private Expr parseExpr() {
        return new Expr(parseAddExpr());
    }

    private ExprAdd parseAddExpr() {
        ExprAdd expr = new ExprAdd(parseMulExpr());
        while (among(TokenType.Plus, TokenType.Minus)) {
            Token token = next();
            ExprAdd.Op op = token.is(TokenType.Plus) ? ExprAdd.Op.ADD : ExprAdd.Op.SUB;
            expr.add(op, parseMulExpr());
        }
        return expr;
    }

    private ExprMul parseMulExpr() {
        ExprMul expr = new ExprMul(parseUnaryExpr());
        while (among(TokenType.Mult, TokenType.Div, TokenType.Mod)) {
            Token token = next();
            ExprMul.Op op =
                token.is(TokenType.Mult) ? ExprMul.Op.MUL :
                    token.is(TokenType.Div) ? ExprMul.Op.DIV :
                        ExprMul.Op.MOD;
            expr.add(op, parseUnaryExpr());
        }
        return expr;
    }

    private ExprUnary parseUnaryExpr() {
        if (among(TokenType.Plus, TokenType.Minus, TokenType.Not)) {
            Token token = next();
            ExprUnaryUnary.Op op =
                token.is(TokenType.Plus) ? ExprUnaryUnary.Op.Plus :
                    token.is(TokenType.Minus) ? ExprUnaryUnary.Op.Minus :
                        ExprUnaryUnary.Op.Not;
            return new ExprUnaryUnary(op, parseUnaryExpr());
        }
        if (among(TokenType.Ident) && among(1, TokenType.LParen)) {
            ExprUnaryCall call = new ExprUnaryCall(consume(TokenType.Ident).value());
            consume(TokenType.LParen);
            while (!checkConsume(TokenType.RParen)) {
                call.addParam(parseExpr());
                checkConsume(TokenType.Comma);
            }
            return call;
        }
        return new ExprUnaryPrimary(parsePrimaryExpr());
    }

    private ExprPrimary parsePrimaryExpr() {
        if (checkConsume(TokenType.LParen)) {
            Expr expr = parseExpr();
            consume(TokenType.RParen);
            return new ExprPrimary(expr);
        }
        if (among(TokenType.IntConst)) {
            return new ExprPrimary(new Number(Integer.parseInt(next().value())));
        }
        return new ExprPrimary(parseLVal());
    }

    private Cond parseCond() {
        return new Cond(parseLOrExpr());
    }

    private CondOr parseLOrExpr() {
        CondOr cond = new CondOr();
        do {
            cond.addCondAnd(parseLAndExpr());
        } while (checkConsume(TokenType.Or));
        return cond;
    }

    private CondAnd parseLAndExpr() {
        CondAnd cond = new CondAnd();
        do {
            cond.addCondEqu(parseEqExpr());
        } while (checkConsume(TokenType.And));
        return cond;
    }

    private CondEqu parseEqExpr() {
        CondEqu cond = new CondEqu(parseRelExpr());
        while (among(TokenType.Eq, TokenType.Neq)) {
            Token token = next();
            CondEqu.Op op = token.is(TokenType.Eq) ? CondEqu.Op.Eq : CondEqu.Op.Ne;
            cond.add(op, parseRelExpr());
        }
        return cond;
    }

    private CondRel parseRelExpr() {
        CondRel cond = new CondRel(parseAddExpr());
        while (among(TokenType.Lt, TokenType.Gt, TokenType.Leq, TokenType.Geq)) {
            Token token = next();
            CondRel.Op op =
                token.is(TokenType.Lt) ? CondRel.Op.Lt :
                    token.is(TokenType.Gt) ? CondRel.Op.Gt :
                        token.is(TokenType.Leq) ? CondRel.Op.Le :
                            CondRel.Op.Ge;
            cond.add(op, parseAddExpr());
        }
        return cond;
    }

    private LeftValue parseLVal() {
        LeftValue leftValue = new LeftValue(consume(TokenType.Ident).value());
        while (checkConsume(TokenType.LBracket)) {
            leftValue.addIndex(parseExpr());
            consume(TokenType.RBracket);
        }
        return leftValue;
    }

    private ForStmt parseForStmt() {
        LeftValue lVal = parseLVal();
        consume(TokenType.Assign);
        Expr expr = parseExpr();
        return new ForStmt(lVal, expr);
    }

    public CompileUnit emit() {
        return ast;
    }
}
