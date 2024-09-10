package top.swkfk.compiler.frontend.symbol;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.utils.Either;

final public class SymbolVariable extends Symbol {
    private Either<FixedValue, FixedArray> constantValue = null;

    public SymbolVariable(String name, SymbolType type, boolean isGlobal) {
        super(name, type, isGlobal);
    }

    public void setConstantValue(FixedValue value) {
        constantValue = Either.left(value);
    }

    public void setConstantValue(FixedArray value) {
        constantValue = Either.right(value);
    }

    public boolean hasFixedValue() {
        return constantValue != null;
    }
}
