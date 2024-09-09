package top.swkfk.compiler.frontend.symbol;

final public class SymbolVariable extends Symbol {
    public SymbolVariable(String name, Type type, boolean isGlobal) {
        super(name, type, isGlobal);
    }
}
