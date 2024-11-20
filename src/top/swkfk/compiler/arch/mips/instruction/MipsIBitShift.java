package top.swkfk.compiler.arch.mips.instruction;

import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.operand.MipsVirtualRegister;

import java.util.Map;

final public class MipsIBitShift extends MipsInstruction {

    @SuppressWarnings("SpellCheckingInspection")
    public enum X {
        sll, srl, sra, sllv, srlv, srav
    }

    private final X operator;
    private MipsOperand res;
    private MipsOperand lhs, rhs;

    public MipsIBitShift(X operator, MipsOperand res, MipsOperand lhs, MipsOperand rhs) {
        this.operator = operator;
        this.res = res;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public MipsOperand[] getOperands() {
        return new MipsOperand[] {res, lhs, rhs};
    }

    @Override
    public MipsVirtualRegister[] getUseVirtualRegisters() {
        return getVirtualRegisters(lhs, rhs);
    }

    @Override
    public MipsVirtualRegister[] getDefVirtualRegisters() {
        return getVirtualRegisters(res);
    }

    @Override
    public void fillPhysicalRegister(Map<MipsVirtualRegister, MipsPhysicalRegister> map) {
        res = fillPhysicalRegister(res, map);
        lhs = fillPhysicalRegister(lhs, map);
        rhs = fillPhysicalRegister(rhs, map);
    }

    @Override
    protected String toMips() {
        return operator + "\t" + res + ", " + lhs + ", " + rhs;
    }
}
