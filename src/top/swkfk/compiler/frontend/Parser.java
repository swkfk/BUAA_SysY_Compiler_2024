package top.swkfk.compiler.frontend;

import top.swkfk.compiler.frontend.ast.CompileUnit;
import top.swkfk.compiler.frontend.ast.block.Block;
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
import top.swkfk.compiler.frontend.ast.misc.LeftValue;
import top.swkfk.compiler.frontend.ast.misc.Number;
import top.swkfk.compiler.frontend.token.Token;
import top.swkfk.compiler.frontend.token.TokenStream;
import top.swkfk.compiler.frontend.token.TokenType;
import top.swkfk.compiler.utils.ParserWatcher;

import java.util.LinkedList;
import java.util.List;

public class Parser {
    /**
     * SHALL NOT consume the token without recording via methods of <code>Parser</code>.
     */
    private final TokenStream tokens;
    private final ParserWatcher watcher;
    private final CompileUnit ast;

    /**
     * Consume the token. Prepared for recording.
     *
     * @param type The specified token type.
     * @return The consumed token.
     */
    private Token consume(TokenType type) {
        Token token = tokens.consume(type);
        watcher.add(token.toString());
        return token;
    }

    /**
     * Check and consume the token if it is among the given types. Prepared for recording.
     *
     * @param types The specified token types.
     * @return Whether the token is among the given types.
     */
    private boolean checkConsume(TokenType... types) {
        Token token = tokens.checkConsume(types);
        if (token != null) {
            watcher.add(token.toString());
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
        Token token = tokens.next();
        watcher.add(token.toString());
        return token;
    }

    public Parser(TokenStream tokens, ParserWatcher watcher) {
        this.tokens = tokens;
        this.watcher = watcher;
        this.ast = new CompileUnit();
    }

    public Parser parse() {
        while (!tokens.eof()) {
            if (tokens.peek(1).is(TokenType.SpMain)) {
                // Main Function
                ast.setMainFunc(parseMainFuncDef());
            } else if (tokens.peek(2).is(TokenType.LParen)) {
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
        while (!next().among(TokenType.RParen)) {
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
        boolean isArray = next().among(TokenType.LBracket);
        FuncFormalParam param = new FuncFormalParam(paramType, name, isArray);
        while (checkConsume(TokenType.LBracket)) {
            if (next().among(TokenType.RBracket)) {
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
        while (!next().among(TokenType.RBrace)) {
            varInitValue.addSubInitializer(parseVarInitial());
            checkConsume(TokenType.Comma);
        }
        consume(TokenType.RBrace);
        return varInitValue;
    }

    private Block parseBlock() {
        return null;
    }

    private Expr parseExpr() {
        return new Expr(parseAddExpr());
    }

    private ExprAdd parseAddExpr() {
        ExprAdd expr = new ExprAdd(parseMulExpr());
        while (tokens.among(TokenType.Plus, TokenType.Minus)) {
            Token token = next();
            ExprAdd.Op op = token.is(TokenType.Plus) ? ExprAdd.Op.ADD : ExprAdd.Op.SUB;
            expr.add(op, parseMulExpr());
        }
        return expr;
    }

    private ExprMul parseMulExpr() {
        ExprMul expr = new ExprMul(parseUnaryExpr());
        while (tokens.among(TokenType.Mult, TokenType.Div, TokenType.Mod)) {
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
        if (tokens.among(TokenType.Plus, TokenType.Minus, TokenType.Not)) {
            Token token = next();
            ExprUnaryUnary.Op op =
                token.is(TokenType.Plus) ? ExprUnaryUnary.Op.Plus :
                    token.is(TokenType.Minus) ? ExprUnaryUnary.Op.Minus :
                        ExprUnaryUnary.Op.Not;
            return new ExprUnaryUnary(op, parseUnaryExpr());
        }
        if (tokens.among(TokenType.Ident) && tokens.peek(1).among(TokenType.LParen)) {
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
        if (tokens.among(TokenType.IntConst)) {
            return new ExprPrimary(new Number(Integer.parseInt(next().value())));
        }
        return new ExprPrimary(parseLVal());
    }

    private LeftValue parseLVal() {
        LeftValue leftValue = new LeftValue(consume(TokenType.Ident).value());
        while (checkConsume(TokenType.LBracket)) {
            leftValue.addIndex(parseExpr());
            consume(TokenType.RBracket);
        }
        return leftValue;
    }

    public CompileUnit emit() {
        return ast;
    }
}
