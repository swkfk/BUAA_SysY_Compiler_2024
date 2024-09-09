package top.swkfk.compiler.frontend.ast.statement;

import top.swkfk.compiler.frontend.ast.logical.Cond;
import top.swkfk.compiler.frontend.ast.misc.ForStmt;

final public class StmtFor extends Stmt {
    private final ForStmt init;
    private final Cond condition;
    private final ForStmt update;
    private final Stmt body;

    public StmtFor(ForStmt init, Cond condition, ForStmt update, Stmt body) {
        super(Type.For);
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }

    public ForStmt getInit() {
        return init;
    }

    public Cond getCondition() {
        return condition;
    }

    public ForStmt getUpdate() {
        return update;
    }

    public Stmt getBody() {
        return body;
    }
}
