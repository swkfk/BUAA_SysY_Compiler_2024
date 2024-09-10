package top.swkfk.compiler.frontend.symbol.type;

final class TyInt extends SymbolType {
    private final int bits;

    public TyInt(int bits) {
        this.bits = bits;
    }

    @Override
    public boolean is(String type) {
        return type.equalsIgnoreCase("int") || type.equalsIgnoreCase("i" + bits);
    }

    @Override
    public int sizeof() {
        return (bits + 7) / 8;
    }

    @Override
    public String toString() {
        return "i" + bits;
    }

    public boolean equals(Object other) {
        if (other instanceof TyInt) {
            return bits == ((TyInt) other).bits;
        }
        return false;
    }
}
