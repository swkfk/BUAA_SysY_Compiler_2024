package top.swkfk.compiler.arch.mips;

import top.swkfk.compiler.arch.mips.instruction.MipsInstruction;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.helpers.GlobalCounter;
import top.swkfk.compiler.utils.DualLinkedList;

final public class MipsBlock extends MipsOperand {
    private final static GlobalCounter counter = new GlobalCounter();
    private final String name;

    private final DualLinkedList<MipsInstruction> instructions = new DualLinkedList<>();

    public MipsBlock() {
        name = "L" + counter.get();
    }

    public void addInstruction(MipsInstruction instruction) {
        new DualLinkedList.Node<>(instruction).insertIntoTail(instructions);
    }

    @Override
    public String toString() {
        return name;
    }

    public String toMips() {
        StringBuilder sb = new StringBuilder();
        sb.append("  ").append(name).append(":\n");
        for (DualLinkedList.Node<MipsInstruction> node : instructions) {
            sb.append("    ").append(node.getData()).append("\n");
        }
        return sb.toString();
    }
}
