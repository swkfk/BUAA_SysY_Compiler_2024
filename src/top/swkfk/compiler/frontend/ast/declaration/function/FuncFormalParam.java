package top.swkfk.compiler.frontend.ast.declaration.function;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.declaration.BasicType;
import top.swkfk.compiler.frontend.ast.expression.ExprConst;
import top.swkfk.compiler.frontend.symbol.SymbolVariable;
import top.swkfk.compiler.frontend.token.Token;

import java.util.LinkedList;
import java.util.List;

final public class FuncFormalParam extends ASTNode {
    private final BasicType type;
    private final Token identifier;
    private SymbolVariable symbol = null;
    /**
     * List of indices for the parameter. It is <code>null</code> if the parameter is not an array.
     * Otherwise, it is an array and the first element shall be <code>null</code>.
     */
    private final List<ExprConst> indices;

    public FuncFormalParam(BasicType type, Token identifier, boolean isArray) {
        this.type = type;
        this.identifier = identifier;
        if (isArray) {
            this.indices = new LinkedList<>();
        } else {
            this.indices = null;
        }
    }

    public void addIndex(ExprConst index) {
        assert indices != null : "The parameter is not an array.";
        assert !indices.isEmpty() || index == null : "The first index shall be null.";
        indices.add(index);
    }

    public List<ExprConst> getIndices() {
        return indices == null ? List.of() : indices;
    }

    public Token getIdentifier() {
        return identifier;
    }

    @Override
    protected String getName() {
        return "<FuncFParam>";
    }

    public void setSymbol(SymbolVariable symbol) {
        this.symbol = symbol;
    }
}
