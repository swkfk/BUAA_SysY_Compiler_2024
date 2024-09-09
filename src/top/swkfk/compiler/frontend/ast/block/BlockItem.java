package top.swkfk.compiler.frontend.ast.block;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.declaration.object.Decl;
import top.swkfk.compiler.frontend.ast.statement.Stmt;

import java.util.List;

final public class BlockItem extends ASTNode {
    public enum Type {
        Decl, Stmt
    }

    private final Type type;
    private final Object item;

    public BlockItem(Decl decl) {
        this.type = Type.Decl;
        this.item = decl;
    }

    public BlockItem(Stmt stmt) {
        this.type = Type.Stmt;
        this.item = stmt;
    }

    public Type getType() {
        return type;
    }

    public Object getItem() {
        return item;
    }

    @Override
    protected String getName() {
        return "<BlockItem>";
    }
}
