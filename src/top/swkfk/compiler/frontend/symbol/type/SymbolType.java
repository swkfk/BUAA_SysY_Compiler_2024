package top.swkfk.compiler.frontend.symbol.type;

import top.swkfk.compiler.frontend.ast.declaration.function.FuncType;

abstract public class SymbolType {

    abstract public boolean is(String type);

    /**
     * 获取类型占用的大小
     * @return 字节数
     */
    abstract public int sizeof();

    abstract public String toString();

    public static SymbolType from(FuncType funcType) {
        if (funcType.is(FuncType.Type.Int)) {
            return Ty.I32;
        }
        if (funcType.is(FuncType.Type.Char)) {
            return Ty.I8;
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

    /**
     * 判断两个类型是否兼容，要么相同，要么都是 int
     * @param other 另一个类型
     * @return 是否兼容
     */
    public boolean compatible(SymbolType other) {
        return this.equals(other) || (this.is("int") && other.is("int"));
    }

    /**
     * 获取最终的基础类型，即去掉所有的指针和数组
     * @return 最终的基础类型
     */
    public SymbolType getFinalBaseType() {
        if (this.is("array")) {
            return ((TyArray) this).getBase().getFinalBaseType();
        } else if (this.is("ptr")) {
            return ((TyPtr) this).getBase().getFinalBaseType();
        } else {
            return this;
        }
    }
}
