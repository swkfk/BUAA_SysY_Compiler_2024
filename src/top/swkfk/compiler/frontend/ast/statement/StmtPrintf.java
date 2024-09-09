package top.swkfk.compiler.frontend.ast.statement;

import top.swkfk.compiler.frontend.ast.expression.Expr;

import java.util.LinkedList;
import java.util.List;

final public class StmtPrintf extends Stmt {
    private final String format;
    private final List<Expr> args;

    public StmtPrintf(String format) {
        super(Type.Printf);
        this.format = format;
        this.args = new LinkedList<>();
    }

    public void addArg(Expr arg) {
        args.add(arg);
    }
}
