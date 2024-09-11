package top.swkfk.compiler.frontend.ast.statement;

import top.swkfk.compiler.frontend.ast.expression.Expr;
import top.swkfk.compiler.frontend.token.Token;

import java.util.LinkedList;
import java.util.List;

final public class StmtPrintf extends Stmt {
    Token token;
    private final String format;
    private final List<Expr> args;

    public StmtPrintf(Token tk, String format) {
        super(Type.Printf);
        this.token = tk;
        this.format = format;
        this.args = new LinkedList<>();
    }

    public void addArg(Expr arg) {
        args.add(arg);
    }

    public List<Expr> getArgs() {
        return args;
    }

    public int getFormatArgCount() {
        int c = 0;
        for (int i = 0; i < format.length(); i++) {
            if (format.charAt(i) == '%') {
                c++;
            }
        }
        return c;
    }

    public Token getToken() {
        return token;
    }
}
