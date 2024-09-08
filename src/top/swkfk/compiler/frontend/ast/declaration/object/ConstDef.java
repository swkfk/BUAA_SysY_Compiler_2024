package top.swkfk.compiler.frontend.ast.declaration.object;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.expression.ExprConst;

import java.util.List;

final public class ConstDef extends ASTNode {
    private final String identifier;
    private final List<ExprConst> indices;
    private final ConstInitValue initial;

    /**
     * Why the three arguments are needed at a time? Because the initial is needed in any case.
     * @param identifier The const's name
     * @param indices The indices, empty means no indices
     * @param initial The initial value
     */
    public ConstDef(String identifier, List<ExprConst> indices, ConstInitValue initial) {
        this.identifier = identifier;
        this.indices = indices;
        this.initial = initial;
    }

    @Override
    protected String getName() {
        return "<ConstDef>";
    }
}
