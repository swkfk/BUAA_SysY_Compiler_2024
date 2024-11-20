package top.swkfk.compiler.arch.mips.instruction;

import top.swkfk.compiler.arch.mips.MipsBlock;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.operand.MipsVirtualRegister;

import java.util.Map;

final public class MipsIBrEqu extends MipsInstruction {
    public enum X {
        beq, bne
    }

    private final X operator;
    private MipsOperand lhs, rhs;
    private final MipsBlock target;

    public MipsIBrEqu(X operator, MipsOperand lhs, MipsOperand rhs, MipsBlock target) {
        this.operator = operator;
        this.lhs = lhs;
        this.rhs = rhs;
        this.target = target;
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

    @Override
    protected String toMips() {
        return operator.toString() + "\t" + lhs + ", " + rhs + ", " + target;
    }
}
