package top.swkfk.compiler.frontend.ast.declaration.object;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.declaration.BasicType;

import java.util.LinkedList;
import java.util.List;

final public class VarDecl extends ASTNode {
    private final BasicType type;
    private final List<VarDef> defs;

    public VarDecl(BasicType type) {
        this.type = type;
        this.defs = new LinkedList<>();
    }

    public void addDef(VarDef def) {
        defs.add(def);
    }

    @Override
    protected String getName() {
        return "<VarDecl>";
    }
}
