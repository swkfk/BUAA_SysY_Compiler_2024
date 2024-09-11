package top.swkfk.compiler.frontend.ast.expression;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;

final public class ExprUnaryPrimary extends ExprUnary {
    private final ExprPrimary primary;

    public ExprUnaryPrimary(ExprPrimary primary) {
        super(Type.Primary);
        this.primary = primary;
    }

    public ExprPrimary getPrimary() {
        return primary;
    }

    @Override
    public int calculateConst() {
        return primary.calculateConst();
    }

    @Override
    public SymbolType calculateType() {
        return primary.calculateType();
    }
}
