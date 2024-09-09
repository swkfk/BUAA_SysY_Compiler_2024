package top.swkfk.compiler.frontend.ast.declaration.object;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.expression.ExprConst;

import java.util.LinkedList;
import java.util.List;

final public class ConstInitValue extends ASTNode {
    public enum Type {
        Initializer, SubInitializer
    }

    private final Type type;
    private final ExprConst expr;
    private final List<ConstInitValue> subInitializers;

    public ConstInitValue(ExprConst expr) {
        this.type = Type.Initializer;
        this.expr = expr;
        this.subInitializers = null;
    }

    public ConstInitValue() {
        this.type = Type.SubInitializer;
        this.expr = null;
        this.subInitializers = new LinkedList<>();
    }

    public void addSubInitializer(ConstInitValue subInitializer) {
        assert subInitializers != null : "Only sub-initializer can add a sub-initializer.";
        subInitializers.add(subInitializer);
    }

    @Override
    protected String getName() {
        return "<ConstInitVal>";
    }
}
