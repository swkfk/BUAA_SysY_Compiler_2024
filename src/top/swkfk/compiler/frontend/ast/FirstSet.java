package top.swkfk.compiler.frontend.ast;

import top.swkfk.compiler.frontend.ast.expression.Expr;
import top.swkfk.compiler.frontend.token.TokenType;

import java.util.Set;

final public class FirstSet {
    public static Set<TokenType> getFirst(Class<? extends ASTNode> node) {
        if (node == Expr.class) {
            return Set.of(
                TokenType.Plus, TokenType.Minus, TokenType.Not,
                TokenType.LParen, TokenType.Ident,
                TokenType.IntConst, TokenType.CharConst
            );
        }
        throw new RuntimeException("Not implemented for " + node);
    }
}
