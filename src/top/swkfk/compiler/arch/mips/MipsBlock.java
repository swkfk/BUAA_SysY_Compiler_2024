package top.swkfk.compiler.arch.mips;

import top.swkfk.compiler.arch.mips.instruction.MipsInstruction;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.helpers.Comments;
import top.swkfk.compiler.helpers.GlobalCounter;
import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.utils.DualLinkedList;

final public class MipsBlock extends MipsOperand {
    private final static GlobalCounter counter = new GlobalCounter();
    private final String name;

    public final Comments comment = new Comments("# ");

    /**
     * The comment for the next instruction. After {@link MipsBlock#addInstruction} called, this will
     * be appended to the instruction and cleared.
     */
    public String reservedComment = null;

    private final DualLinkedList<MipsInstruction> instructions = new DualLinkedList<>();

    public MipsBlock() {
        this(null);
    }

    public MipsBlock(BasicBlock block) {
        name = "L" + counter.get();
        if (block != null) {
            comment.append("%" + block.getName()).append(": ").append(block.comment.getComment());
        }
    }

    public void addInstruction(MipsInstruction instruction) {
        new DualLinkedList.Node<>(instruction).insertIntoTail(instructions);
        if (reservedComment != null) {
            instruction.comment.append(reservedComment);
            reservedComment = null;
        }
    }

    @Override
    public String toString() {
        return name;
    }

    public String toMips() {
        StringBuilder sb = new StringBuilder();
        sb.append("  ").append(name).append(":\t\t").append(comment).append("\n");
        for (DualLinkedList.Node<MipsInstruction> node : instructions) {
            sb.append(node.getData()).append("\n");
        }
        return sb.toString();
    }
}
