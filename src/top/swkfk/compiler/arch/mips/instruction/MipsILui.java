package top.swkfk.compiler.arch.mips.instruction;

import top.swkfk.compiler.arch.mips.operand.MipsImmediate;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.operand.MipsVirtualRegister;

import java.util.Map;

final public class MipsILui extends MipsInstruction {
    private MipsOperand res;
    private MipsImmediate imm;

    public MipsILui(MipsOperand res, MipsImmediate imm) {
        this.res = res;
        this.imm = imm;
    }

    @Override
    public MipsOperand[] getOperands() {
        return new MipsOperand[] {
            res, imm
        };
    }

    @Override
    public MipsVirtualRegister[] getUseVirtualRegisters() {
        return getVirtualRegisters();
    }

    @Override
    public MipsVirtualRegister[] getDefVirtualRegisters() {
        return getVirtualRegisters(res);
    }

    @Override
    public void fillPhysicalRegister(Map<MipsVirtualRegister, MipsPhysicalRegister> map) {
        fillPhysicalRegister(res, map);
    }

    @Override
    protected String toMips() {
        return "lui\t" + res + ", " + imm;
    }
}
