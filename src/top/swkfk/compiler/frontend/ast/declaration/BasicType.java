package top.swkfk.compiler.frontend.ast.declaration;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.frontend.symbol.type.Ty;
import top.swkfk.compiler.frontend.token.TokenType;

final public class BasicType extends ASTNode {
    enum Type {
        Int, Char
    }

    private final Type type;

    private BasicType(Type type) {
        this.type = type;
    }

    public static BasicType from(TokenType type) {
        return switch (type) {
            case Int -> new BasicType(Type.Int);
            case Char -> new BasicType(Type.Char);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    public SymbolType into() {
        return switch (type) {
            case Int -> Ty.I32;
            case Char -> Ty.I8;
        };
    }

    @Override
    protected String getName() {
        return "<BType>";
    }
}
