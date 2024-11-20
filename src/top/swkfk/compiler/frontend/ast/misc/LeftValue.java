package top.swkfk.compiler.frontend.ast.misc;

import top.swkfk.compiler.frontend.ast.ASTNode;
import top.swkfk.compiler.frontend.ast.expression.Expr;
import top.swkfk.compiler.frontend.symbol.FixedArray;
import top.swkfk.compiler.frontend.symbol.FixedValue;
import top.swkfk.compiler.frontend.symbol.SymbolVariable;
import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.frontend.symbol.type.Ty;
import top.swkfk.compiler.frontend.symbol.type.TyArray;
import top.swkfk.compiler.frontend.symbol.type.TyPtr;
import top.swkfk.compiler.frontend.token.Token;
import top.swkfk.compiler.utils.Either;

import java.util.LinkedList;
import java.util.List;

final public class LeftValue extends ASTNode {
    private final Token identifier;
    private SymbolVariable symbol = null;
    private final List<Expr> indices;

    public LeftValue(Token identifier) {
        this.identifier = identifier;
        this.indices = new LinkedList<>();
    }

    public LeftValue(SymbolVariable symbol, List<Expr> indices) {
        this.identifier = null;
        this.symbol = symbol;
        this.indices = indices;
    }

    public void addIndex(Expr index) {
        indices.add(index);
    }

    public Token getIdentifier() {
        return identifier;
    }

    public List<Expr> getIndices() {
        return indices;
    }

    public SymbolVariable getSymbol() {
        return symbol;
    }

    @Override
    protected String getName() {
        return "<LVal>";
    }

    public void setSymbol(SymbolVariable symbol) {
        this.symbol = symbol;
    }

    public int calculateConst() {
        if (symbol == null) {
            throw new RuntimeException("Symbol is not set to the LeftValue");
        }
        if (!symbol.hasFixedValue()) {
            throw new RuntimeException("Symbol does not have a fixed value in LeftValue");
        }
        Either<FixedValue, FixedArray> value = symbol.getConstantValue();
        if (value.isLeft()) {
            return value.getLeft().getValue();
        } else {
            FixedArray array = value.getRight();
            return array.get(indices);
        }
    }

    public SymbolType calculateType() {
        SymbolType result = symbol.getType();
        if (result.is("ptr")) {
            result = new TyArray(((TyPtr) result).getBase(), 0);
        }
        if (result.is("array")) {
            for (var ignore : indices) {
                result = ((TyArray) result).getBase();
            }
        }
        if (result.is("array")) {
            result = new TyPtr(((TyArray) result).getBase());
        }
        return result;
    }
}
