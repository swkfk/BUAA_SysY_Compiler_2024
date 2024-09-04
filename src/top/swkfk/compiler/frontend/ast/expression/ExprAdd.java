package top.swkfk.compiler.frontend.ast.expression;

import top.swkfk.compiler.frontend.ast.ASTNode;

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

    public void add(Op op, ExprMul right) {
        ops.add(op);
        rights.add(right);
    }

    @Override
    protected String getName() {
        return "<AddExp>";
    }
}
