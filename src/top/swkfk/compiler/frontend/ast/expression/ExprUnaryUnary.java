package top.swkfk.compiler.frontend.ast.expression;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;

final public class ExprUnaryUnary extends ExprUnary {
    public enum Op {
        Plus, Minus, Not;

        public String toString() {
            return "<UnaryOp>";
        }
    }

    private final Op op;
    private final ExprUnary expr;

    public ExprUnaryUnary(Op op, ExprUnary expr) {
        super(Type.Unary);
        this.op = op;
        this.expr = expr;
    }

    @Override
    public int calculateConst() {
        int value = expr.calculateConst();
        return switch (op) {
            case Plus -> value;
            case Minus -> -value;
            default -> throw new RuntimeException("Unexpected Not in Const Expr");
        };
    }

    @Override
    public SymbolType calculateType() {
        return expr.calculateType();
    }

    public ExprUnary getExpr() {
        return expr;
    }
}
