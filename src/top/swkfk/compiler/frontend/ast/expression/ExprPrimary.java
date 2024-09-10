package top.swkfk.compiler.frontend.ast.expression;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.misc.LeftValue;
import top.swkfk.compiler.frontend.ast.misc.Number;

final public class ExprPrimary extends ASTNode {
    public enum Type {
        Expr, LVal, Number
    }

    private final Type type;
    private final Object value;

    public ExprPrimary(Expr expr) {
        this.type = Type.Expr;
        this.value = expr;
    }

    public ExprPrimary(LeftValue left) {
        this.type = Type.LVal;
        this.value = left;
    }

    public ExprPrimary(Number number) {
        this.type = Type.Number;
        this.value = number;
    }

    public Type getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public int calculateConst() {
        return switch (type) {
            case Expr -> ((Expr) value).getExpr().calculateConst();
            case LVal -> ((LeftValue) value).calculateConst();
            case Number -> ((Number) value).getValue();
        };
    }

    @Override
    protected String getName() {
        return "<PrimaryExp>";
    }
}
