package top.swkfk.compiler.arch.mips.operand;

import top.swkfk.compiler.helpers.GlobalCounter;

final public class MipsVirtualRegister extends MipsOperand {
    private static final GlobalCounter counter = new GlobalCounter();
    private final int id;

    public MipsVirtualRegister() {
        id = counter.get();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MipsVirtualRegister) {
            return id == ((MipsVirtualRegister) obj).id;
        }
        return false;
    }

    @Override
    public String toString() {
        return "$$vir" + id;
    }
}
