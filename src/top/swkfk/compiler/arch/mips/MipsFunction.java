package top.swkfk.compiler.arch.mips;

import top.swkfk.compiler.arch.mips.instruction.MipsIPhi;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.utils.DualLinkedList;

public class MipsFunction extends MipsOperand {
    private final String name;
    private final DualLinkedList<MipsBlock> blocks = new DualLinkedList<>();

    public MipsFunction(String name) {
        this.name = name;
    }

    public MipsBlock getEntryBlock() {
        return blocks.getHead().getData();
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
}
