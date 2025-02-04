package top.swkfk.compiler.frontend.ast.expression;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.helpers.ConstValue;

import java.util.LinkedList;
import java.util.List;

final public class ExprAdd extends ASTNode {
    public enum Op {
        ADD, SUB
    }

    private final ExprMul left;
    private final List<Op> ops;
    private final List<ExprMul> rights;

    public ExprAdd(ExprMul left) {
        this.left = left;
        this.ops = new LinkedList<>();
        this.rights = new LinkedList<>();
    }

    public ExprMul getLeft() {
        return left;
    }

    public List<ExprMul> getRights() {
        return rights;
    }

    public void add(Op op, ExprMul right) {
        ops.add(op);
        rights.add(right);
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

    public List<Op> getOps() {
        return ops;
    }

    @Override
    protected String getName() {
        return "<AddExp>";
    }
}
