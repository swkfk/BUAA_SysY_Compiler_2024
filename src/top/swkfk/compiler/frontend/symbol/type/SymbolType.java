package top.swkfk.compiler.frontend.symbol.type;

import top.swkfk.compiler.frontend.ast.declaration.function.FuncType;

abstract public class SymbolType {
    private final SymbolQualifier qualifier = new SymbolQualifier();

    public void setConst() {
        qualifier.setConst();
    }

    public boolean isConst() {
        return qualifier.isConst();
    }

    abstract public boolean is(String type);

    abstract public int sizeof();

    abstract public String toString();

    public static SymbolType from(FuncType funcType) {
        if (funcType.is(FuncType.Type.Int)) {
            return Ty.I32;
        }
        return Ty.Void;
    }
}
