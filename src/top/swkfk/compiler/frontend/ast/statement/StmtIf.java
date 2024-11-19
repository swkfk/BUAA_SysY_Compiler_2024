package top.swkfk.compiler.frontend.ast.statement;

import top.swkfk.compiler.frontend.Navigation;
import top.swkfk.compiler.frontend.ast.logical.Cond;

final public class StmtIf extends Stmt {
    private final Cond condition;
    private final Stmt thenStmt;
    private final Stmt elseStmt;
    private final Navigation navigation;

    public StmtIf(Cond condition, Stmt thenStmt, Stmt elseStmt, Navigation navigation) {
        super(Type.If);
        this.condition = condition;
        this.thenStmt = thenStmt;
        this.elseStmt = elseStmt;
        this.navigation = navigation;
    }

    public StmtIf(Cond condition, Stmt thenStmt, Navigation navigation) {
        this(condition, thenStmt, null, navigation);
    }

    public Cond getCondition() {
        return condition;
    }

    public Stmt getThenStmt() {
        return thenStmt;
    }

    public Stmt getElseStmt() {
        return elseStmt;
    }

    public Navigation getNavigation() {
        return navigation;
    }
}
