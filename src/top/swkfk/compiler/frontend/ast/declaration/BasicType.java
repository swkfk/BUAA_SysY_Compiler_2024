package top.swkfk.compiler.frontend.ast.declaration;

import top.swkfk.compiler.frontend.ast.ASTNode;

final public class BasicType extends ASTNode {
    public BasicType() {
    }

    @Override
    protected String getName() {
        return "<BType>";
    }
}
