package top.swkfk.compiler.frontend.ast.misc;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.expression.Expr;
import top.swkfk.compiler.frontend.symbol.SymbolVariable;

import java.util.LinkedList;
import java.util.List;

final public class LeftValue extends ASTNode {
    private final String identifier;
    private SymbolVariable symbol = null;
    private final List<Expr> indices;

    public LeftValue(String identifier) {
        this.identifier = identifier;
        this.indices = new LinkedList<>();
    }

    public void addIndex(Expr index) {
        indices.add(index);
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    protected String getName() {
        return "<LVal>";
    }

    public void setSymbol(SymbolVariable symbol) {
        this.symbol = symbol;
    }
}
