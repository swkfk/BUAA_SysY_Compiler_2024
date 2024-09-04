package top.swkfk.compiler.frontend.ast.declaration.object;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.expression.ExprConst;

import java.util.LinkedList;
import java.util.List;

final public class VarDef extends ASTNode {
    private final String identifer;
    private final List<ExprConst> indices;
    /**
     * Initial value of the variable. <code>null</code> if not initialized.
     */
    private final VarInitValue initial;

    public VarDef(String identifer, VarInitValue initial) {
        this.identifer = identifer;
        this.indices = new LinkedList<>();
        this.initial = initial;
    }

    public VarDef(String identifer) {
        this(identifer, null);
    }

    public void addIndex(ExprConst index) {
        indices.add(index);
    }

    @Override
    protected String getName() {
        return "<VarDef>";
    }
}
