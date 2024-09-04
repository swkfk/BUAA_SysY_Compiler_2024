package top.swkfk.compiler.frontend.ast.expression;

final public class ExprUnaryUnary extends ExprUnary {
    public enum Op {
        Plus, Minus, Not
    }

    private final Op op;

    protected ExprUnaryUnary(Op op) {
        super(Type.Unary);
        this.op = op;
    }
}
