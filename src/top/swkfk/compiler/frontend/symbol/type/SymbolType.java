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

    public static String getDisplayString(SymbolType type) {
        if (type.is("i32")) {
            return "Int";
        } else if (type.is("i8")) {
            return "Char";
        } else if (type.is("array")) {
            return getDisplayString(((TyArray) type).getBase()) + "Array";
        } else if (type.is("ptr")) {
            return getDisplayString(((TyPtr) type).getBase()) + "Array";
        } else if (type.is("void")) {
            return "Void";
        } else {
            throw new RuntimeException("Unknown type: " + type);
        }
    }

    public boolean compatible(SymbolType other) {
        return this.equals(other) || (this.is("int") && other.is("int"));
    }
}
