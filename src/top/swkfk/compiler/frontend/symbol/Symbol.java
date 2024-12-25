package top.swkfk.compiler.frontend.symbol;

import top.swkfk.compiler.frontend.symbol.type.SymbolQualifier;
import top.swkfk.compiler.frontend.symbol.type.SymbolType;

/**
 * 符号的基类，派生类有变量与函数等
 */
public class Symbol implements Comparable<Symbol> {
    /// 符号内部的计数器，用于生成唯一的标识符
    private static int count = 0;

    /// 符号所在的符号表的索引
    private final int symbolTableIndex;
    /// 符号在符号表中的索引
    private final int index;

    /// 符号的名称
    private final String name;
    /// 符号的 mangle 名，目前无实际意义，只是用于调试输出而已
    private final String mangle;
    /// 符号的类型
    private final SymbolType type;
    /// 是否是全局符号
    private final boolean isGlobal;

    /// 符号的限定符，目前只支持 const
    private final SymbolQualifier qualifier = new SymbolQualifier();

    public void setConst() {
        qualifier.setConst();
    }

    public boolean isConst() {
        return qualifier.isConst();
    }

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
