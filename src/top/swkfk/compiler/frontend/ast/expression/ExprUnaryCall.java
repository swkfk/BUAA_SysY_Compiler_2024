package top.swkfk.compiler.frontend.ast.expression;

import top.swkfk.compiler.frontend.ast.misc.FuncRealParams;
import top.swkfk.compiler.frontend.symbol.SymbolFunction;

import java.util.List;

final public class ExprUnaryCall extends ExprUnary {
    private final String identifier;
    private SymbolFunction symbol = null;
    /**
     * The parameters of the function call. If the inner list is empty, the &lt;FuncRParams&gt; must
     * be non-exist.
     */
    private final FuncRealParams params;

    public ExprUnaryCall(String identifier, FuncRealParams params) {
        super(Type.Call);
        this.identifier = identifier;
        this.params = params;
    }

    public void setSymbol(SymbolFunction symbol) {
        this.symbol = symbol;
    }

    public String getIdentifier() {
        return identifier;
    }

    public List<Expr> getParams() {
        return params.getParams();
    }
}
