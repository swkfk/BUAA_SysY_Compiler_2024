package top.swkfk.compiler.frontend.symbol;

class Symbol {

    public enum Type {
        Int, Void
    }

    private static int count = 0;

    private final String name;
    private final String mangle;
    private final Type type;
    private final boolean isGlobal;

    public Symbol(String name, Type type, boolean isGlobal) {
        this.name = name;
        this.type = type;
        this.mangle = "__" + type + "_" + name + "_" + count++;
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
