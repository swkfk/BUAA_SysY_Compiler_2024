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

    @Override
    public Integer numbering() {
        if (opcode.swappable()) {
            return (getOperand(0).hashCode() ^ getOperand(1).hashCode()) * opcode.ordinal();
        } else {
            return (getOperand(0).hashCode() ^ (7 * getOperand(1).hashCode())) * opcode.ordinal() + 1;
        }
    }

    @Override
    public boolean numberingEquals(User obj) {
        if (!(obj instanceof IBinary other)) {
            return false;
        }
        if (this.opcode != other.opcode) {
            return false;
        }
        if (this.opcode.swappable()) {
            return (this.getOperand(0).equals(other.getOperand(0)) && this.getOperand(1).equals(other.getOperand(1))) ||
                (this.getOperand(0).equals(other.getOperand(1)) && this.getOperand(1).equals(other.getOperand(0)));
        } else {
            return this.getOperand(0).equals(other.getOperand(0)) && this.getOperand(1).equals(other.getOperand(1));
        }
    }
}
