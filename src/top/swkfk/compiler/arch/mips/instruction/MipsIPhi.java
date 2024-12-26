package top.swkfk.compiler.arch.mips.instruction;

import top.swkfk.compiler.arch.mips.MipsBlock;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.operand.MipsVirtualRegister;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Mips 层面的 phi 指令，用于 SSA 表示，这是 remove phi 的对象，实际不应该出现在最终的汇编代码中
 */
final public class MipsIPhi extends MipsInstruction {
    private MipsOperand res;
    private final List<MipsOperand> operands;
    private final List<MipsBlock> sources;

    public MipsIPhi(MipsOperand result) {
        this.res = result;
        this.operands = new LinkedList<>();
        this.sources = new LinkedList<>();
    }

    public MipsOperand getResult() {
        return res;
    }

    public void addOperand(MipsOperand operand, MipsBlock source) {
        operands.add(operand);
        sources.add(source);
    }

    public MipsOperand getOperand(int index) {
        return operands.get(index);
    }

    public MipsBlock getSource(int index) {
        return sources.get(index);
    }

    public int getOperandsSize() {
        return operands.size();
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
        res = fillPhysicalRegister(res, map);
        operands.replaceAll(operand -> fillPhysicalRegister(operand, map));
    }

    @Override
    protected String toMips() {
        StringBuilder sb = new StringBuilder("phi " + res + " ");
        for (int i = 0; i < operands.size(); i++) {
            sb.append("[ ").append(operands.get(i)).append(", ").append(sources.get(i)).append(" ] ");
        }
        return sb.toString();
    }

    public void replaceOperand(int i, MipsOperand newOperand) {
        operands.set(i, newOperand);
    }
}
