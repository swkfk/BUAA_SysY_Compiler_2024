package top.swkfk.compiler.frontend.symbol;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.utils.Either;

/**
 * 变量符号，主要包含了变量的（潜在的）常量值，以及在 IR 中的值（指针）
 */
final public class SymbolVariable extends Symbol {
    /// 常量值，包含多重类型，但只针对 const 变量
    private Either<FixedValue, FixedArray> constantValue = null;

    /**
     * The value in IR of this variable. It is a POINTER to the memory location.
     */
    private Value value;

    /**
     * Whether this variable is from a parameter of a function.
     *
     * @see top.swkfk.compiler.llvm.value.instruction.IGep#IGep(Value, Value, boolean) 
     */
    private boolean fromParam = false;

    public SymbolVariable(String name, SymbolType type, boolean isGlobal, int symbolTableIndex) {
        super(name, type, isGlobal, symbolTableIndex);
    }

    public void setConstantValue(FixedValue value) {
        constantValue = Either.left(value);
    }

    public void setConstantValue(FixedArray value) {
        constantValue = Either.right(value);
    }

    public boolean hasNoFixedValue() {
        return constantValue == null;
    }

    public Either<FixedValue, FixedArray> getConstantValue() {
        return constantValue;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public Value getValue() {
        return value;
    }

    public void setFromParam() {
        this.fromParam = true;
    }

    public boolean isFromParam() {
        return fromParam;
    }

    @Override
    public String toDebugString() {
        if (hasNoFixedValue()) {
            return super.toDebugString();
        }
        return super.toDebugString() + " = " + constantValue;
    }

    @Override
    public String toString() {
        String type = (isConst() ? "Const" : "") + SymbolType.getDisplayString(getType());
        return super.toString() + " " + type;
    }
}
