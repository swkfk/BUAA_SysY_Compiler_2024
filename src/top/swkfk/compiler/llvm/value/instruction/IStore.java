package top.swkfk.compiler.llvm.value.instruction;

import top.swkfk.compiler.frontend.symbol.type.TyPtr;
import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.Value;

final public class IStore extends User {

    public IStore(Value value, Value pointer) {
        super("", null);
        assert pointer.getType().is("ptr") : "Only pointer type can be stored";
        assert ((TyPtr) pointer.getType()).getBase().equals(value.getType()) : "Type mismatch";
        addOperand(value);
        addOperand(pointer);
    }

    public Value getPointer() {
        return getOperand(1);
    }

    public Value getValue() {
        return getOperand(0);
    }

    @Override
    public String toLLVM() {
        return "store " + getOperand(0) + ", " + getOperand(1);
    }
}
