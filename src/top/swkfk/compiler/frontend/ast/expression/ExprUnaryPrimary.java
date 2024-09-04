package top.swkfk.compiler.frontend.ast.expression;

final public class ExprUnaryPrimary extends ExprUnary {
    private final ExprPrimary primary;

    protected ExprUnaryPrimary(ExprPrimary primary) {
        super(Type.Primary);
        this.primary = primary;
    }
}
