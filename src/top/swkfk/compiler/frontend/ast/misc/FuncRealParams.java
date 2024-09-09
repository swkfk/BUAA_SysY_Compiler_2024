package top.swkfk.compiler.frontend.ast.misc;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.expression.Expr;

import java.util.LinkedList;
import java.util.List;

final public class FuncRealParams extends ASTNode {
    private final List<Expr> params;

    public FuncRealParams() {
        this.params = new LinkedList<>();
    }

    public void addParam(Expr param) {
        params.add(param);
    }

    public List<Expr> getParams() {
        return params;
    }

    @Override
    protected String getName() {
        return "<FuncRParams>";
    }
}
