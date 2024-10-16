package top.swkfk.compiler.frontend.symbol;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;

public class Symbol implements Comparable<Symbol> {
    private static int count = 0;

    private final int symbolTableIndex;
    private final int index;

    private final String name;
    private final String mangle;
    private final SymbolType type;
    private final boolean isGlobal;

    protected Symbol(String name, SymbolType type, boolean isGlobal, int symbolTableIndex) {
        this.name = name;
        this.type = type;
        this.index = count++;
        this.mangle = (isGlobal ? "__g_" : "__l_") + name + "_" + index;
        this.isGlobal = isGlobal;
        this.symbolTableIndex = symbolTableIndex;
    }

    public String toDebugString() {
        return name + "<" + mangle + "#" + symbolTableIndex + "." + index + "> : " + type;
    }

    public String getMangle() {
        return mangle;
    }

    public String getName() {
        return name;
    }

    public SymbolType getType() {
        return type;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    @Override
    public int compareTo(Symbol symbol) {
        if (symbolTableIndex == symbol.symbolTableIndex) {
            return Integer.compare(index, symbol.index);
        }
        return Integer.compare(symbolTableIndex, symbol.symbolTableIndex);
    }

    @Override
    public String toString() {
        return symbolTableIndex + " " + name;
    }
}
