package top.swkfk.compiler.arch.mips.instruction;

import top.swkfk.compiler.arch.mips.operand.MipsImmediate;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.operand.MipsVirtualRegister;

import java.util.Map;

@SuppressWarnings("SpellCheckingInspection")
final public class MipsIMultDiv extends MipsInstruction {

    public enum X {
        mult, multu, div, divu
    }

    private final X operator;
    private MipsOperand lhs, rhs;

    public MipsIMultDiv(X operator, MipsOperand lhs, MipsOperand rhs) {
        this.operator = operator;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public MipsOperand[] getOperands() {
        return new MipsOperand[] {lhs, rhs};
    }

    @Override
    public MipsVirtualRegister[] getUseVirtualRegisters() {
        return getVirtualRegisters(lhs, rhs);
    }

    @Override
    public MipsVirtualRegister[] getDefVirtualRegisters() {
        return getVirtualRegisters();
    }

    @Override
    public void fillPhysicalRegister(Map<MipsVirtualRegister, MipsPhysicalRegister> map) {
        lhs = fillPhysicalRegister(lhs, map);
        rhs = fillPhysicalRegister(rhs, map);
    }

    public X getOperator() {
        return operator;
    }

    @Override
    protected String toMips() {
        return operator + "\t" + lhs + ", " + rhs;
    }
}
