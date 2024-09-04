package top.swkfk.compiler.frontend.ast.declaration.function;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.declaration.BasicType;
import top.swkfk.compiler.frontend.ast.expression.ExprConst;

import java.util.LinkedList;
import java.util.List;

final public class FuncFormalParam extends ASTNode {
    private final BasicType type;
    private final String identifier;
    /**
     * List of indices for the parameter. It is <code>null</code> if the parameter is not an array.
     * Otherwise, it is an array and the first element shall be <code>null</code>.
     */
    private final List<ExprConst> indices;

    public FuncFormalParam(BasicType type, String identifier, boolean isArray) {
        this.type = type;
        this.identifier = identifier;
        if (isArray) {
            this.indices = new LinkedList<>();
        } else {
            this.indices = null;
        }
    }

    public void addIndex(ExprConst index) {
        assert indices != null : "The parameter is not an array.";
        assert !indices.isEmpty() || index == null : "The first index shall be null.";
        indices.add(index);
    }

    @Override
    protected String getName() {
        return "<FuncFParam>";
    }
}
