package top.swkfk.compiler.llvm.value.constants;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.frontend.symbol.type.Ty;

final public class ConstInteger extends Constant {

    private final int value;

    public ConstInteger(int value) {
        super(String.valueOf(value), Ty.I32);
        this.value = value;
    }

    public ConstInteger(char value) {
        super(String.valueOf(value), Ty.I8);
        this.value = value;
    }

    public ConstInteger(int value, SymbolType type) {
        super(String.valueOf(value), type);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConstInteger) {
            return value == ((ConstInteger) obj).value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }

    public final static ConstInteger zero = new ConstInteger(0);
    public final static ConstInteger logicZero = new ConstInteger(0, Ty.I1);
    public final static ConstInteger logicOne = new ConstInteger(1, Ty.I1);
}
