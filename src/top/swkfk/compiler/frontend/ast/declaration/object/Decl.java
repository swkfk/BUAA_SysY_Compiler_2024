package top.swkfk.compiler.frontend.ast.declaration.object;

import top.swkfk.compiler.frontend.ast.ASTNode;

final public class Decl extends ASTNode {
    public enum Type {
        Const, Variable
    }

    private final Type type;
    private final Object declaration;

    public Decl(ConstDecl constDecl) {
        this.type = Type.Const;
        this.declaration = constDecl;
    }

    public Decl(VarDecl varDecl) {
        this.type = Type.Variable;
        this.declaration = varDecl;
    }

    public Type getType() {
        return type;
    }

    public Object getDeclaration() {
        return declaration;
    }

    @Override
    protected String getName() {
        return "<Decl>";
    }
}
