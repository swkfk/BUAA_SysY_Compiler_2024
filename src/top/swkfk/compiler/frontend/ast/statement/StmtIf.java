package top.swkfk.compiler.frontend.ast.statement;

import top.swkfk.compiler.frontend.ast.logical.Cond;

final public class StmtIf extends Stmt {
    private final Cond condition;
    private final Stmt thenStmt;
    private final Stmt elseStmt;

    public StmtIf(Cond condition, Stmt thenStmt, Stmt elseStmt) {
        super(Type.If);
        this.condition = condition;
        this.thenStmt = thenStmt;
        this.elseStmt = elseStmt;
    }

    public StmtIf(Cond condition, Stmt thenStmt) {
        this(condition, thenStmt, null);
    }
}
