package top.swkfk.compiler.arch.mips.instruction;

import top.swkfk.compiler.arch.mips.MipsBlock;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.operand.MipsVirtualRegister;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

final public class MipsIPhi extends MipsInstruction {
    private final MipsOperand res;
    private final List<MipsOperand> operands;
    private final List<MipsBlock> sources;

    public MipsIPhi(MipsOperand result) {
        this.res = result;
        this.operands = new LinkedList<>();
        this.sources = new LinkedList<>();
    }

    public void addOperand(MipsOperand operand, MipsBlock source) {
        operands.add(operand);
        sources.add(source);
    }

    @Override
    public MipsOperand[] getOperands() {
        return new LinkedList<MipsOperand>() {{
            add(res);
            addAll(operands);
        }}.toArray(new MipsOperand[0]);
    }

    @Override
    public MipsVirtualRegister[] getUseVirtualRegisters() {
        return getVirtualRegisters(operands.toArray(new MipsOperand[0]));
    }

    @Override
    public MipsVirtualRegister[] getDefVirtualRegisters() {
        return getVirtualRegisters(res);
    }

    @Override
    public void fillPhysicalRegister(Map<MipsVirtualRegister, MipsPhysicalRegister> map) {
        fillPhysicalRegister(res, map);
        for (MipsOperand operand : operands) {
            fillPhysicalRegister(operand, map);
        }
    }

    @Override
    protected String toMips() {
        StringBuilder sb = new StringBuilder("phi " + res + " ");
        for (int i = 0; i < operands.size(); i++) {
            sb.append("[ ").append(operands.get(i)).append(", ").append(sources.get(i)).append(" ] ");
        }
        return sb.toString();
    }
}
