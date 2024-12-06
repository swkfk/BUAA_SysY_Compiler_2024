package top.swkfk.compiler.arch.mips.instruction;

import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.operand.MipsVirtualRegister;

import java.util.Map;

final public class MipsILoadAddress extends MipsInstruction {
    MipsOperand address;
    MipsOperand destination;

    public MipsILoadAddress(MipsOperand address, MipsOperand destination) {
        this.address = address;
        this.destination = destination;
    }

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
        return getVirtualRegisters(destination);
    }

    @Override
    public void fillPhysicalRegister(Map<MipsVirtualRegister, MipsPhysicalRegister> map) {
        fillPhysicalRegister(destination, map);
    }

    @Override
    protected String toMips() {
        return "la\t" + destination + ", " + address;
    }
}
