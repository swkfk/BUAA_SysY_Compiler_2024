package top.swkfk.compiler.frontend.ast.logical;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.expression.ExprAdd;

import java.util.LinkedList;
import java.util.List;

final public class CondRel extends ASTNode {

    public enum Op {
        Lt, Gt, Le, Ge
    }

    private final ExprAdd left;
    private final List<Op> ops;
    private final List<ExprAdd> rights;

    public CondRel(ExprAdd left) {
        this.left = left;
        this.ops = new LinkedList<>();
        this.rights = new LinkedList<>();
    }

    public ExprAdd getLeft() {
        return left;
    }

    public List<ExprAdd> getRights() {
        return rights;
    }

    public void add(Op op, ExprAdd right) {
        ops.add(op);
        rights.add(right);
    }

    @Override
    protected String getName() {
        return "<RelExp>";
    }
}
