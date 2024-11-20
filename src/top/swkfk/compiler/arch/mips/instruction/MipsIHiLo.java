package top.swkfk.compiler.arch.mips.instruction;

import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.operand.MipsVirtualRegister;

import java.util.Map;

final public class MipsIHiLo extends MipsInstruction {

    @SuppressWarnings("SpellCheckingInspection")
    public enum X {
        mfhi, mflo, mthi, mtlo
    }

    private final X operator;
    private MipsOperand operand;

    public MipsIHiLo(X operator, MipsOperand operand) {
        this.operator = operator;
        this.operand = operand;
    }

    @Override
    public MipsOperand[] getOperands() {
        return new MipsOperand[]{operand};
    }

    @Override
    public MipsVirtualRegister[] getUseVirtualRegisters() {
        return getVirtualRegisters(operator == X.mfhi || operator == X.mflo ? operand : null);
    }

    @Override
    public MipsVirtualRegister[] getDefVirtualRegisters() {
        return getVirtualRegisters(operator == X.mthi || operator == X.mtlo ? operand : null);
    }

    @Override
    public void fillPhysicalRegister(Map<MipsVirtualRegister, MipsPhysicalRegister> map) {
        operand = fillPhysicalRegister(operand, map);
    }

    @Override
    protected String toMips() {
        return operator + "\t" + operand;
    }
}
