package top.swkfk.compiler.llvm.value;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;

final public class Function extends Value {

    /**
     * Function is a value that represents a function.
     * @param name function name without '@' or mangling
     * @param type function's return type
     */
    public Function(String name, SymbolType type) {
        super(name, type);
        assert type.is("void") || type.is("i32") : "Function return type must be void or i32";
    }
}
