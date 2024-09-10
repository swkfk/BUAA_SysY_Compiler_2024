package top.swkfk.compiler.frontend.symbol;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;

public class Symbol {
    private static int count = 0;

    private final String name;
    private final String mangle;
    private final SymbolType type;
    private final boolean isGlobal;

    protected Symbol(String name, SymbolType type, boolean isGlobal) {
        this.name = name;
        this.type = type;
        this.mangle = (isGlobal ? "__g_" : "__l_") + name + "_" + count++;
        this.isGlobal = isGlobal;
    }

    public String toString() {
        return name + "<" + mangle + "> : " + type;
    }

    public String getMangle() {
        return mangle;
    }

    public String getName() {
        return name;
    }
}
