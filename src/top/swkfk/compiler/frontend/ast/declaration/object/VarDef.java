package top.swkfk.compiler.frontend.ast.declaration.object;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.expression.ExprConst;
import top.swkfk.compiler.frontend.symbol.SymbolVariable;

import java.util.LinkedList;
import java.util.List;

final public class VarDef extends ASTNode {
    private final String identifer;
    private SymbolVariable symbol = null;
    private final List<ExprConst> indices;
    /**
     * Initial value of the variable. <code>null</code> if not initialized.
     */
    private VarInitValue initial;

    public VarDef(String identifer) {
        this.identifer = identifer;
        this.indices = new LinkedList<>();
        this.initial = null;
    }

    public void setInitial(VarInitValue initial) {
        this.initial = initial;
    }

    public void addIndex(ExprConst index) {
        indices.add(index);
    }

    public String getIdentifier() {
        return identifer;
    }

    @Override
    protected String getName() {
        return "<VarDef>";
    }

    public void setSymbol(SymbolVariable symbol) {
        this.symbol = symbol;
    }
}
