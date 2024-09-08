package top.swkfk.compiler.frontend.ast.expression;

final public class ExprUnaryUnary extends ExprUnary {
    public enum Op {
        Plus, Minus, Not
    }

    private final Op op;
    private final ExprUnary expr;

    public ExprUnaryUnary(Op op, ExprUnary expr) {
        super(Type.Unary);
        this.op = op;
        this.expr = expr;
    }
}
