package top.swkfk.compiler.frontend.ast.declaration.object;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.expression.ExprConst;

import java.util.LinkedList;
import java.util.List;

final public class ConstInitValue extends ASTNode {


    public enum Type {
        Initializer, SubInitializer, StringConst
    }
    private final Type type;

    private final ExprConst expr;
    private final String stringConst;
    private final List<ConstInitValue> subInitializers;
    public ConstInitValue(ExprConst expr) {
        this.type = Type.Initializer;
        this.expr = expr;
        this.stringConst = null;
        this.subInitializers = null;
    }

    public ConstInitValue() {
        this.type = Type.SubInitializer;
        this.expr = null;
        this.stringConst = null;
        this.subInitializers = new LinkedList<>();
    }

    public ConstInitValue(String stringConst) {
        this.type = Type.StringConst;
        this.expr = null;
        this.stringConst = stringConst;
        this.subInitializers = null;
    }

    public Type getType() {
        return type;
    }

    public ExprConst getExpr() {
        return expr;
    }

    public List<ConstInitValue> getSubInitializers() {
        return subInitializers;
    }

    public String getStringConst() {
        return stringConst;
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
