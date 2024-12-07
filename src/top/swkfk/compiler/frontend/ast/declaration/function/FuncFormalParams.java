package top.swkfk.compiler.frontend.ast.declaration.function;

import top.swkfk.compiler.frontend.ast.ASTNode;

import java.util.LinkedList;
import java.util.List;

final public class FuncFormalParams extends ASTNode {
    private final List<FuncFormalParam> paramList;

    public FuncFormalParams() {
        this.paramList = new LinkedList<>();
    }

    public void addParam(FuncFormalParam param) {
        paramList.add(param);
    }

    public List<FuncFormalParam> getParamList() {
        return paramList;
    }

    @Override
    protected String getName() {
        return "<FuncFParams>";
    }
}
