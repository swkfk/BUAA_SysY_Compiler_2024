package top.swkfk.compiler.frontend.ast.declaration.object;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.expression.ExprConst;
import top.swkfk.compiler.frontend.symbol.SymbolVariable;
import top.swkfk.compiler.frontend.token.Token;

import java.util.LinkedList;
import java.util.List;

final public class VarDef extends ASTNode {
    private final Token identifier;
    private SymbolVariable symbol = null;
    private final List<ExprConst> indices;
    /**
     * Initial value of the variable. <code>null</code> if not initialized.
     */
    private VarInitValue initial;  // 注意，这里是语法树节点，不是符号表中的初始值

    public VarDef(Token identifier) {
        this.identifier = identifier;
        this.indices = new LinkedList<>();
        this.initial = null;
    }

    public void setInitial(VarInitValue initial) {
        this.initial = initial;
    }

    public void addIndex(ExprConst index) {
        indices.add(index);
    }

    public Token getIdentifier() {
        return identifier;
    }

    public VarInitValue getInitial() {
        return initial;
    }

    public List<ExprConst> getIndices() {
        return indices;
    }

    @Override
    protected String getName() {
        return "<VarDef>";
    }

    public void setSymbol(SymbolVariable symbol) {
        this.symbol = symbol;
    }

    public SymbolVariable getSymbol() {
        return symbol;
    }
}
