package top.swkfk.compiler.frontend.symbol.type;

final class TyVoid extends SymbolType {

    @Override
    public boolean is(String type) {
        return type.equalsIgnoreCase("void");
    }

    @Override
    public int sizeof() {
        return 0;
    }

    @Override
    public String toString() {
        return "void";
    }

    public boolean equals(Object other) {
        return other instanceof TyVoid;
    }
}
