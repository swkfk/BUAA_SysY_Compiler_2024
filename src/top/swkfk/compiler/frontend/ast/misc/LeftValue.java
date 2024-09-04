package top.swkfk.compiler.frontend.ast.misc;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.expression.Expr;

import java.util.LinkedList;
import java.util.List;

final public class LeftValue extends ASTNode {
    private final String identifier;
    private final List<Expr> indices;

    public LeftValue(String identifier) {
        this.identifier = identifier;
        this.indices = new LinkedList<>();
    }

    public void addIndex(Expr index) {
        indices.add(index);
    }

    @Override
    protected String getName() {
        return "<LVal>";
    }
}
