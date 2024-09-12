package top.swkfk.compiler.llvm.value.instruction;

import top.swkfk.compiler.frontend.symbol.type.TyPtr;
import top.swkfk.compiler.llvm.User;
import top.swkfk.compiler.llvm.value.Value;

final public class ILoad extends User {

    public ILoad(Value pointer) {
        super("%" + Value.counter.get(), pointer.getType());
        assert pointer.getType().is("ptr") : "Only pointer type can be loaded";
        addOperand(pointer);
    }

    @Override
    public String toLLVM() {
        return getName() + " = load " +
            ((TyPtr) getType()).getBase() + ", " + getOperand(0);
    }
}
