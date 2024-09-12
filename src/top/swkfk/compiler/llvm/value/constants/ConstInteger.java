package top.swkfk.compiler.llvm.value.constants;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.frontend.symbol.type.Ty;

final public class ConstInteger extends Constant {

    private final int value;

    public ConstInteger(int value) {
        super(String.valueOf(value), Ty.I32);
        this.value = value;
    }
}
