package top.swkfk.compiler.llvm.value.constants;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.frontend.symbol.type.Ty;

final public class ConstInteger extends Constant {

    private final int value;

    public ConstInteger(int value) {
        super(String.valueOf(value), Ty.I32);
        this.value = value;
    }

    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConstInteger) {
            return value == ((ConstInteger) obj).value;
        }
        return false;
    }

    public final static ConstInteger zero = new ConstInteger(0);
}
