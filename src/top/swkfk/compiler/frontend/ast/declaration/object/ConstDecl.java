package top.swkfk.compiler.frontend.ast.declaration.object;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.declaration.BasicType;

import java.util.LinkedList;
import java.util.List;

final public class ConstDecl extends ASTNode {
    private final BasicType type;
    private final List<ConstDef> defs;

    public ConstDecl(BasicType type) {
        this.type = type;
        this.defs = new LinkedList<>();
    }

    public void addDef(ConstDef def) {
        defs.add(def);
    }

    public List<ConstDef> getDefs() {
        return defs;
    }

    @Override
    protected String getName() {
        return "<ConstDecl>";
    }
}
