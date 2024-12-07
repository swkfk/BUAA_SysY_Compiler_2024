package top.swkfk.compiler.llvm.value.instruction;

import top.swkfk.compiler.frontend.symbol.type.TyPtr;
import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.Value;

final public class ILoad extends User {

    public ILoad(Value pointer) {
        super("%" + Value.counter.get(), ((TyPtr) pointer.getType()).getBase());
        assert pointer.getType().is("ptr") : "Only pointer type can be loaded";
        addOperand(pointer);
    }

    public Value getPointer() {
        return getOperand(0);
    }

    @Override
    public String toLLVM() {
        return getName() + " = load " + getType() + ", " + getOperand(0);
    }
}
