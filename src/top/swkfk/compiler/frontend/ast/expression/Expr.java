package top.swkfk.compiler.frontend.ast.expression;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.symbol.type.SymbolType;

final public class Expr extends ASTNode {

    private final ExprAdd expr;

    public Expr(ExprAdd expr) {
        this.expr = expr;
    }

    public ExprAdd getExpr() {
        return expr;
    }

    public int calculateConst() {
        return expr.calculateConst();
    }

    /**
     * The chain of SymbolType
     * @return The type
     */
    public SymbolType calculateType() {
        return expr.calculateType();
    }

    @Override
    protected String getName() {
        return "<Exp>";
    }
}
