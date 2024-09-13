package top.swkfk.compiler.frontend.symbol;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.utils.Either;

final public class SymbolVariable extends Symbol {
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

    public SymbolVariable(String name, SymbolType type, boolean isGlobal) {
        super(name, type, isGlobal);
    }

    public void setConstantValue(FixedValue value) {
        constantValue = Either.left(value);
    }

    public void setConstantValue(FixedArray value) {
        constantValue = Either.right(value);
    }

    public boolean hasFixedValue() {
        return constantValue != null;
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

    @Override
    public String toString() {
        if (!hasFixedValue()) {
            return super.toString();
        }
        return super.toString() + " = " + constantValue;
    }
}
