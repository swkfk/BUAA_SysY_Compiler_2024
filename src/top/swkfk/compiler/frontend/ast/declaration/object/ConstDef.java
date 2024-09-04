package top.swkfk.compiler.frontend.ast.declaration.object;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.expression.ExprConst;

import java.util.List;

final public class ConstDef extends ASTNode {
    private final String identifer;
    private final List<ExprConst> indices;
    private final ConstInitValue initial;

    public ConstDef(String identifer, List<ExprConst> indices, ConstInitValue initial) {
        this.identifer = identifer;
        this.indices = indices;
        this.initial = initial;
    }

    @Override
    protected String getName() {
        return "<ConstDef>";
    }
}
