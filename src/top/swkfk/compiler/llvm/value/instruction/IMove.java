package top.swkfk.compiler.llvm.value.instruction;

import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.Value;

/**
 * Although the 'move' will be translated into an 'addi %%, %%, 0', we still use move in that:
 * <li>We do not need to care the real type.</li>
 * <li>The semantic is more clear when doing the optimization.</li>
 */
final public class IMove extends User {

    public IMove(Value value) {
        super("%" + Value.counter.get(), value.getType());
        addOperand(value);
    }

    @Override
    public String toLLVM() {
        return getName() + " = move " + getOperand(0);
    }
}
