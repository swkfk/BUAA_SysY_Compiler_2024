package top.swkfk.compiler.frontend.ast.expression;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.helpers.ConstValue;

import java.util.LinkedList;
import java.util.List;

final public class ExprMul extends ASTNode {
    public enum Op {
        MUL, DIV, MOD
    }

    private final ExprUnary left;
    private final List<Op> ops;
    private final List<ExprUnary> rights;

    public ExprMul(ExprUnary left) {
        this.left = left;
        this.ops = new LinkedList<>();
        this.rights = new LinkedList<>();
    }

    public ExprUnary getLeft() {
        return left;
    }

    public List<ExprUnary> getRights() {
        return rights;
    }

    public void add(Op op, ExprUnary expr) {
        this.ops.add(op);
        this.rights.add(expr);
    }

    public int calculateConst() {
        int value = left.calculateConst();
        for (int i = 0; i < rights.size(); i++) {
            value = ConstValue.calculate(value, rights.get(i).calculateConst(), ConstValue.from(ops.get(i)));
        }
        return value;
    }

    public SymbolType calculateType() {
        return left.calculateType();
    }

    @Override
    protected String getName() {
        return "<MulExp>";
    }
}
