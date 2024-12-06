package top.swkfk.compiler.arch.mips;

import top.swkfk.compiler.arch.mips.instruction.MipsIBinary;
import top.swkfk.compiler.arch.mips.instruction.MipsIPhi;
import top.swkfk.compiler.arch.mips.instruction.MipsInstruction;
import top.swkfk.compiler.arch.mips.operand.MipsImmediate;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.utils.DualLinkedList;

public class MipsFunction extends MipsOperand {
    private final String name;
    private final DualLinkedList<MipsBlock> blocks = new DualLinkedList<>();
    private MipsBlock entry, exit;
    private int stackSize = 0;

    public MipsFunction(String name) {
        this.name = name;
    }

    public int getStackSize() {
        return stackSize;
    }

    public void setStackSize(int stackSize) {
        this.stackSize = stackSize;
    }

    public void enlargeStackSize(int stackSize) {
        this.stackSize += stackSize;
    }

    public void setKeyBlock(MipsBlock entry, MipsBlock exit) {
        this.entry = entry;
        this.exit = exit;
    }

    public MipsBlock getEntryBlock() {
        return entry;
    }

    public MipsBlock getExitBlock() {
        return exit;
    }

    public void addBlock(MipsBlock block) {
        new DualLinkedList.Node<>(block).insertIntoTail(blocks);
    }

    public DualLinkedList<MipsBlock> getBlocks() {
        return blocks;
    }

    @Override
    public String toString() {
        return name;
    }

    public String toMips() {
        StringBuilder sb = new StringBuilder();
        // sb.append(name).append(":\n");
        for (DualLinkedList.Node<MipsBlock> node : blocks) {
            sb.append(node.getData().toMips()).append("\n");
        }
        return sb.toString();
    }

    public void fillStackSize() {
        new DualLinkedList.Node<MipsInstruction>(
            new MipsIBinary(MipsIBinary.X.addiu, MipsPhysicalRegister.sp, MipsPhysicalRegister.sp, new MipsImmediate(getStackSize()))
        ).insertBefore(exit.getInstructions().getTail());
        entry.addInstructionAfter(
            new MipsIBinary(MipsIBinary.X.addiu, MipsPhysicalRegister.sp, MipsPhysicalRegister.sp, new MipsImmediate(-getStackSize())),
            instruction -> instruction instanceof MipsIBinary && instruction.getOperands()[0] == MipsPhysicalRegister.fp
        );
    }
}
