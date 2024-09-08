package top.swkfk.compiler.frontend.ast.declaration.object;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.expression.Expr;

import java.util.LinkedList;
import java.util.List;

final public class VarInitValue extends ASTNode {
    public enum Type {
        Initializer, SubInitializer
    }

    private final Type type;
    private final Expr expr;
    private final List<VarInitValue> subInitializers;

    public VarInitValue(Expr expr) {
        this.type = Type.Initializer;
        this.expr = expr;
        this.subInitializers = null;
    }

    public VarInitValue() {
        this.type = Type.SubInitializer;
        this.expr = null;
        this.subInitializers = new LinkedList<>();
    }

    public void addSubInitializer(VarInitValue subInitializer) {
        assert subInitializers != null : "Only sub-initializer can add a sub-initializer.";
        subInitializers.add(subInitializer);
    }

    @Override
    protected String getName() {
        return "<InitVal>";
    }
}
