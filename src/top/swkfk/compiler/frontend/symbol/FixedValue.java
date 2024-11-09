package top.swkfk.compiler.frontend.symbol;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.frontend.symbol.type.Ty;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.llvm.value.constants.ConstInteger;

final public class FixedValue {
    private final int value;
    private final SymbolType type;

    public FixedValue(int value) {
        this(value, Ty.I32);
    }

    public FixedValue(int value, SymbolType type) {
        this.value = value;
        this.type = type;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    public Value into() {
        return new ConstInteger(value, type);
    }
}
