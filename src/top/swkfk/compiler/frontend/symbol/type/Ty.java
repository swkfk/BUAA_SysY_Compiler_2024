package top.swkfk.compiler.frontend.symbol.type;

/**
 * 常用的类型，直接获取，不再需要 new
 */
final public class Ty {
    public static final SymbolType Void = new TyVoid();
    public static final SymbolType I1 = new TyInt(1);
    public static final SymbolType I8 = new TyInt(8);
    public static final SymbolType I32 = new TyInt(32);
}
