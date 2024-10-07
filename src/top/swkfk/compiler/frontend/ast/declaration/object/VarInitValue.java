package top.swkfk.compiler.frontend.ast.declaration.object;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.expression.Expr;
import top.swkfk.compiler.frontend.ast.expression.ExprConst;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

final public class VarInitValue extends ASTNode {
    public enum Type {
        Initializer, SubInitializer, StringConst
    }

    private final Type type;
    private final Expr expr;
    private final String stringConst;
    private final List<VarInitValue> subInitializers;

    public VarInitValue(Expr expr) {
        this.type = Type.Initializer;
        this.expr = expr;
        this.stringConst = null;
        this.subInitializers = null;
    }

    public VarInitValue() {
        this.type = Type.SubInitializer;
        this.expr = null;
        this.stringConst = null;
        this.subInitializers = new LinkedList<>();
    }

    public VarInitValue(String stringConst) {
        this.type = Type.StringConst;
        this.expr = null;
        this.stringConst = stringConst;
        this.subInitializers = null;
    }

    public Expr getExpr() {
        return expr;
    }

    public List<VarInitValue> getSubInitializers() {
        return subInitializers;
    }

    public void addSubInitializer(VarInitValue subInitializer) {
        assert subInitializers != null : "Only sub-initializer can add a sub-initializer.";
        subInitializers.add(subInitializer);
    }

    @Override
    protected String getName() {
        return "<InitVal>";
    }

    public ConstInitValue into() {
        if (type == Type.Initializer) {
            return new ConstInitValue(new ExprConst(Objects.requireNonNull(expr).getExpr()));
        } else {
            ConstInitValue subInitVals = new ConstInitValue();
            for (VarInitValue subInit : Objects.requireNonNull(subInitializers)) {
                subInitVals.addSubInitializer(subInit.into());
            }
            return subInitVals;
        }
    }
}
