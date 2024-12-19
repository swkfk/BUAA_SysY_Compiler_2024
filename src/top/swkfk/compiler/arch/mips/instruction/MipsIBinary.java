package top.swkfk.compiler.arch.mips.instruction;

import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.operand.MipsVirtualRegister;

import java.util.Map;

final public class MipsIBinary extends MipsInstruction {
    @SuppressWarnings("SpellCheckingInspection")
    public enum X {
        addu, subu, and, or, xor, not, slt, sltu,
        addiu, andi, ori, xori, nor, slti, sltiu,
        sle, sge, sgt, seq, sne
    }

    private final X operator;
    private MipsOperand res;
    private MipsOperand lhs, rhs;

    public MipsIBinary(X operator, MipsOperand res, MipsOperand lhs, MipsOperand rhs) {
        this.operator = operator;
        this.res = res;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public X getOperator() {
        return operator;
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
