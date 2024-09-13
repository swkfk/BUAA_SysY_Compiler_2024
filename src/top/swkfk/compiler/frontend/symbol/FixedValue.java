package top.swkfk.compiler.frontend.symbol;

import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.llvm.value.constants.ConstInteger;

final public class FixedValue {
    private final int value;

    public FixedValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    public Value into() {
        return new ConstInteger(value);
    }
}
