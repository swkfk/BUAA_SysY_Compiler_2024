package top.swkfk.compiler.llvm.value.constants;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.llvm.value.Value;

/**
 * Constant is not const, but a value that is constant or literal. Includes integer.
 */
public class Constant extends Value {
    public Constant(String name, SymbolType type) {
        super(name, type);
    }
}
