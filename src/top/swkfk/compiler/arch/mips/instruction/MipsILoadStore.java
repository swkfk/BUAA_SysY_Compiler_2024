package top.swkfk.compiler.arch.mips.instruction;

import top.swkfk.compiler.arch.mips.operand.MipsImmediate;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.operand.MipsVirtualRegister;

import java.util.Map;

final public class MipsILoadStore extends MipsInstruction {

    public enum X {
        lbu, lhu, lw, SEPARATOR, sb, sh, sw
    }

    private final X operator;
    private MipsOperand dstOrSrc;
    private MipsOperand base;
    private MipsImmediate offset;

    public MipsILoadStore(X operator, MipsOperand dstOrSrc, MipsOperand base, MipsImmediate offset) {
        this.operator = operator;
        this.dstOrSrc = dstOrSrc;
        this.base = base;
        this.offset = offset;
    }

    public boolean isLoad() {
        return operator.ordinal() < X.SEPARATOR.ordinal();
    }

    @Override
    public MipsOperand[] getOperands() {
        return new MipsOperand[]{base, offset};
    }

    @Override
    public MipsVirtualRegister[] getUseVirtualRegisters() {
        return getVirtualRegisters(base, isLoad() ? null : dstOrSrc);
    }

    @Override
    public MipsVirtualRegister[] getDefVirtualRegisters() {
        return getVirtualRegisters(isLoad() ? dstOrSrc : null);
    }

    @Override
    public void fillPhysicalRegister(Map<MipsVirtualRegister, MipsPhysicalRegister> map) {
        base = fillPhysicalRegister(base, map);
        dstOrSrc = fillPhysicalRegister(dstOrSrc, map);
    }

    @Override
    protected String toMips() {
        return operator + "\t" + dstOrSrc + ", " + offset + "(" + base + ")";
    }
}
