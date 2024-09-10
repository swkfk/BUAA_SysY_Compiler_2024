package top.swkfk.compiler.frontend.symbol.type;

final public class TyArray extends SymbolType {

    private final SymbolType base;
    private final int length;

    public TyArray(SymbolType base, int length) {
        this.base = base;
        this.length = length;
    }

    @Override
    public boolean is(String type) {
        return type.equalsIgnoreCase(toString()) || type.equalsIgnoreCase("array");
    }

    @Override
    public int sizeof() {
        return base.sizeof() * length;
    }

    @Override
    public String toString() {
        return "[" + length + " x " + base + "]";
    }
}
