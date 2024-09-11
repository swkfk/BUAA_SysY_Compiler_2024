package top.swkfk.compiler.frontend.symbol.type;

final public class TyPtr extends SymbolType {

    private final SymbolType base;

    public TyPtr(SymbolType base) {
        this.base = base;
    }

    public SymbolType getBase() {
        return base;
    }

    @Override
    public boolean is(String type) {
        return type.equalsIgnoreCase(toString()) || type.equalsIgnoreCase("ptr");
    }

    @Override
    public int sizeof() {
        return 4;  // 32-bit pointer
    }

    @Override
    public String toString() {
        return base + "*";
    }
}
