package top.swkfk.compiler.arch.mips.instruction;

import top.swkfk.compiler.arch.mips.MipsBlock;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.operand.MipsVirtualRegister;

import java.util.Map;

final public class MipsIBrZero extends MipsInstruction {

    @SuppressWarnings("SpellCheckingInspection")
    public enum X {
        bgez, bgezal, bgtz, blez, bltz, bltzal
    }

    private final X operator;
    private MipsOperand operand;
    private MipsBlock target;

    public MipsIBrZero(X operator, MipsOperand operand, MipsBlock target) {
        this.operator = operator;
        this.operand = operand;
        this.target = target;
    }

    @Override
    public void replaceJumpTarget(MipsBlock oldBlock, MipsBlock newBlock) {
        if (target == oldBlock) {
            target = newBlock;
        }
    }

    @Override
    public MipsOperand[] getOperands() {
        return new MipsOperand[] {operand, target};
    }

    @Override
    public MipsVirtualRegister[] getUseVirtualRegisters() {
        return getVirtualRegisters(operand);
    }

    @Override
    public MipsVirtualRegister[] getDefVirtualRegisters() {
        return getVirtualRegisters();
    }

    @Override
    public void fillPhysicalRegister(Map<MipsVirtualRegister, MipsPhysicalRegister> map) {
        operand = fillPhysicalRegister(operand, map);
    }

    @Override
    protected String toMips() {
        return operator + "\t" + operand + ", " + target;
    }
}
