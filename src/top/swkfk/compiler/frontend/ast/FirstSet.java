package top.swkfk.compiler.frontend.ast;

import top.swkfk.compiler.frontend.ast.expression.Expr;
import top.swkfk.compiler.frontend.ast.misc.LeftValue;
import top.swkfk.compiler.frontend.token.TokenType;

import java.util.Map;
import java.util.Set;

final public class FirstSet {
    public static Set<TokenType> getFirst(Class<? extends ASTNode> node) {
        return Map.ofEntries(
            Map.entry(Expr.class, Set.of(
                TokenType.Plus, TokenType.Minus, TokenType.Not,
                TokenType.LParen, TokenType.Ident,
                TokenType.IntConst, TokenType.CharConst
            )),
            Map.entry(LeftValue.class, Set.of(
                TokenType.Ident
            ))
        ).getOrDefault(node, Set.of());
    }
}
