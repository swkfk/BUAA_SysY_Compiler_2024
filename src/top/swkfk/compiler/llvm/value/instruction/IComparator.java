package top.swkfk.compiler.llvm.value.instruction;

import top.swkfk.compiler.frontend.symbol.type.Ty;
import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.Value;

final public class IComparator extends User {

    private final BinaryOp opcode;

    public IComparator(BinaryOp opcode, Value[] operands) {
        this(opcode, operands[0], operands[1]);
    }

    public IComparator(BinaryOp opcode, Value lhs, Value rhs) {
        super("%" + Value.counter.get(), Ty.I1);
        assert lhs.getType() == rhs.getType() : "The type of lhs and rhs should be the same";
        assert opcode.ordinal() > BinaryOp.Separator.ordinal() : "The opcode should be a comparator binary operator";
        addOperand(lhs);
        addOperand(rhs);
        this.opcode = opcode;
    }

    public BinaryOp getOpcode() {
        return opcode;
    }

    @Override
    public String toLLVM() {
        return getName() + " = " + opcode.getOpcode() + " " + getOperand(0).getType() + " " +
            getOperand(0).getName() + ", " + getOperand(1).getName();
    }
}
