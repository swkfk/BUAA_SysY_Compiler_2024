package top.swkfk.compiler.frontend.ast.logical;

import top.swkfk.compiler.frontend.ast.ASTNode;

import java.util.LinkedList;
import java.util.List;

final public class CondOr extends ASTNode {
    private List<CondAnd> condAndList;

    public CondOr() {
        this.condAndList = new LinkedList<>();
    }

    public List<CondAnd> getCondAndList() {
        return condAndList;
    }

    public void addCondAnd(CondAnd condAnd) {
        condAndList.add(condAnd);
    }

    @Override
    protected String getName() {
        return "<LOrExp>";
    }
}
