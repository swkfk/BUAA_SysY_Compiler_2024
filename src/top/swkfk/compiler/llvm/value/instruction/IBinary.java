package top.swkfk.compiler.llvm.value.instruction;

import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.Value;

final public class IBinary extends User {

    private final BinaryOp opcode;

    public IBinary(BinaryOp opcode, Value[] operands) {
        this(opcode, operands[0], operands[1]);
    }

    public BinaryOp getOpcode() {
        return opcode;
    }

    public IBinary(BinaryOp opcode, Value lhs, Value rhs) {
        super("%" + Value.counter.get(), lhs.getType());
        assert lhs.getType() == rhs.getType() : "The type of lhs and rhs should be the same";
        assert opcode.ordinal() < BinaryOp.Separator.ordinal() : "The opcode should be a arithmetic binary operator";
        addOperand(lhs);
        addOperand(rhs);
        this.opcode = opcode;
    }

    @Override
    public String toLLVM() {
        return getName() + " = " + opcode.getOpcode() + " " + getType() + " " +
            getOperand(0).getName() + ", " + getOperand(1).getName();
    }
}
