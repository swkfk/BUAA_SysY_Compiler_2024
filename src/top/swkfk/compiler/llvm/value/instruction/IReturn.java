package top.swkfk.compiler.llvm.value.instruction;

import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.llvm.value.Value;

final public class IReturn extends ITerminator {

    public IReturn() {
        super("", null);
    }

    public IReturn(Value ret) {
        super("", null);
        addOperand(ret);
    }

    @Override
    public String toLLVM() {
        StringBuilder sb = new StringBuilder();
        sb.append("ret ");
        if (getOperands().isEmpty()) {
            sb.append("void");
        } else {
            sb.append(getOperand(0));
        }
        return sb.toString();
    }

    @Override
    public BasicBlock[] getSuccessors() {
        return new BasicBlock[0];
    }
}
