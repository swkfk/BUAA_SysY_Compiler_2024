package top.swkfk.compiler.arch.mips.instruction;

import top.swkfk.compiler.arch.mips.MipsBlock;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.operand.MipsVirtualRegister;
import top.swkfk.compiler.helpers.Comments;

import java.util.Map;

public abstract class MipsInstruction {
    public final Comments comment = new Comments("    ## ");

    /**
     * Get the operands list of the instruction.
     *
     * @return The operands list of the instruction.
     */
    public abstract MipsOperand[] getOperands();

    /**
     * Get the operands that the instruction uses.
     *
     * @return The operands that the instruction uses.
     */
    public abstract MipsVirtualRegister[] getUseVirtualRegisters();

    /**
     * Get the virtual register that the instruction defines.
     *
     * @return The operands that the instruction defines.
     */
    public abstract MipsVirtualRegister[] getDefVirtualRegisters();

    public void replaceJumpTarget(MipsBlock oldBlock, MipsBlock newBlock) {
        // Do nothing by default.
    }

    /**
     * Detect and get the virtual registers from the operands.
     * @param operands The operands to be detected.
     * @return The virtual registers that are detected.
     */
    protected MipsVirtualRegister[] getVirtualRegisters(MipsOperand... operands) {
        return java.util.Arrays.stream(operands)
            .filter(operand -> operand instanceof MipsVirtualRegister)
            .map(operand -> (MipsVirtualRegister) operand)
            .toArray(MipsVirtualRegister[]::new);
    }

    /**
     * Replace the virtual register with physical register. If the map does not contain the virtual register,
     * the virtual register will not be replaced.
     *
     * @param map The map that maps the virtual register to physical register.
     */
    public abstract void fillPhysicalRegister(Map<MipsVirtualRegister, MipsPhysicalRegister> map);

    /**
     * Helper function to replace the virtual register with physical register.
     *
     * @param operand The operand to be replaced.
     * @param map     The map that maps the virtual register to physical register.
     * @return The replaced operand.
     */
    protected MipsOperand fillPhysicalRegister(MipsOperand operand, Map<MipsVirtualRegister, MipsPhysicalRegister> map) {
        if (operand instanceof MipsVirtualRegister vir) {
            if (map.containsKey(vir)) {
                return map.get(vir);
            }
        }
        return operand;
    }

    protected abstract String toMips();

    /**
     * Get the string representation of the instruction.
     *
     * @return The MIPS assembly code including the comment of the instruction.
     */
    public final String toString() {
        String comment = this.comment.toString();
        if (comment.isEmpty()) {
            return "    " + toMips();
        } else {
            return comment + "\n    " + toMips();
        }
    }
}
