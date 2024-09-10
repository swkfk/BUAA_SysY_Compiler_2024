package top.swkfk.compiler.frontend.symbol;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;

final public class SymbolVariable extends Symbol {
    public SymbolVariable(String name, SymbolType type, boolean isGlobal) {
        super(name, type, isGlobal);
    }
}
