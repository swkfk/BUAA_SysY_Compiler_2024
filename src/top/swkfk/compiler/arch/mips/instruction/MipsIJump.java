package top.swkfk.compiler.arch.mips.instruction;

import top.swkfk.compiler.arch.mips.MipsBlock;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.operand.MipsVirtualRegister;
import top.swkfk.compiler.utils.Either;

import java.util.Map;

final public class MipsIJump extends MipsInstruction {

    public enum X {
        j, jal, jr
    }

    private final X operator;
    private Either<MipsOperand, MipsBlock> operand;

    public MipsIJump(X operator, MipsOperand operand) {
        assert operator == X.jr : "Jump to register must be jr instead of " + operator;
        this.operator = operator;
        this.operand = Either.left(operand);
    }

    public MipsIJump(X operator, MipsBlock operand) {
        this.operator = operator;
        this.operand = Either.right(operand);
    }

    @Override
    public MipsOperand[] getOperands() {
        if (operand.isLeft()) {
            return new MipsOperand[]{operand.getLeft()};
        } else {
            return new MipsOperand[0];
        }
    }

    @Override
    public MipsVirtualRegister[] getUseVirtualRegisters() {
        return getVirtualRegisters(operand.isLeft() ? operand.getLeft() : null);
    }

    @Override
    public MipsVirtualRegister[] getDefVirtualRegisters() {
        return getVirtualRegisters();
    }

    @Override
    public void fillPhysicalRegister(Map<MipsVirtualRegister, MipsPhysicalRegister> map) {
        if (operand.isLeft()) {
            operand = Either.left(fillPhysicalRegister(operand.getLeft(), map));
        }
    }

    @Override
    public void replaceJumpTarget(MipsBlock oldBlock, MipsBlock newBlock) {
        if (operand.isRight() && operand.getRight() == oldBlock) {
            operand = Either.right(newBlock);
        }
    }

    @Override
    protected String toMips() {
        return operator + "\t" + operand;
    }
}
