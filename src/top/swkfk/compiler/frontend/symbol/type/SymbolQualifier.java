package top.swkfk.compiler.frontend.symbol.type;

/**
 * Qualifier for a type. Only const is supported.
 */
final public class SymbolQualifier {
    private boolean isConst = false;

    public void setConst() {
        isConst = true;
    }

    public boolean isConst() {
        return isConst;
    }
}
