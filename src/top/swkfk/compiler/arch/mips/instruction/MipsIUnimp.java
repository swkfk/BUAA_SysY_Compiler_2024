package top.swkfk.compiler.arch.mips.instruction;

import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.operand.MipsVirtualRegister;

import java.util.Map;

/**
 * 仅仅表示某条指令未实现，实际不应该出现在最终的汇编代码中
 */
@SuppressWarnings("SpellCheckingInspection")
final public class MipsIUnimp extends MipsInstruction {
    @Override
    public MipsOperand[] getOperands() {
        return new MipsOperand[0];
    }

    @Override
    public MipsVirtualRegister[] getUseVirtualRegisters() {
        return new MipsVirtualRegister[0];
    }

    @Override
    public MipsVirtualRegister[] getDefVirtualRegisters() {
        return new MipsVirtualRegister[0];
    }

    @Override
    public void fillPhysicalRegister(Map<MipsVirtualRegister, MipsPhysicalRegister> map) {
    }

    @Override
    protected String toMips() {
        return "unimp";
    }
}
