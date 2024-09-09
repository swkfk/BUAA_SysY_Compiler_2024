package top.swkfk.compiler.frontend.ast.logical;

import top.swkfk.compiler.frontend.ast.ASTNode;

import java.util.LinkedList;
import java.util.List;

final public class CondEqu extends ASTNode {
    public enum Op {
        Eq, Ne
    }

    private final CondRel left;
    private final List<Op> ops;
    private final List<CondRel> rights;

    public CondEqu(CondRel left) {
        this.left = left;
        this.ops = new LinkedList<>();
        this.rights = new LinkedList<>();
    }

    public CondRel getLeft() {
        return left;
    }

    public List<CondRel> getRights() {
        return rights;
    }

    public void add(Op op, CondRel right) {
        ops.add(op);
        rights.add(right);
    }

    @Override
    protected String getName() {
        return "<EqExp>";
    }
}
