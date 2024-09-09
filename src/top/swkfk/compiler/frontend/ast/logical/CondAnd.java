package top.swkfk.compiler.frontend.ast.logical;

import top.swkfk.compiler.frontend.ast.ASTNode;

import java.util.LinkedList;
import java.util.List;

final public class CondAnd extends ASTNode {
    private final List<CondEqu> condEquList;

    public CondAnd() {
        this.condEquList = new LinkedList<>();
    }

    public List<CondEqu> getCondEquList() {
        return condEquList;
    }

    public void addCondEqu(CondEqu condEqu) {
        condEquList.add(condEqu);
    }

    @Override
    protected String getName() {
        return "<LAndExp>";
    }
}
