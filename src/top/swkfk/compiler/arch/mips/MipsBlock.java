package top.swkfk.compiler.arch.mips;

import top.swkfk.compiler.arch.mips.instruction.MipsInstruction;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.helpers.Comments;
import top.swkfk.compiler.helpers.GlobalCounter;
import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.utils.DualLinkedList;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

final public class MipsBlock extends MipsOperand {
    private final static GlobalCounter counter = new GlobalCounter();
    private final String name;

    private final Set<MipsBlock> successors = new HashSet<>();
    private final Set<MipsBlock> predecessors = new HashSet<>();

    public final Comments comment = new Comments("# ");

    /**
     * The comment for the next instruction. After {@link MipsBlock#addInstruction} called, this will
     * be appended to the instruction and cleared.
     */
    public String reservedComment = null;

    private final DualLinkedList<MipsInstruction> instructions = new DualLinkedList<>();

    public MipsBlock(String name) {
        this(null, name);
    }

    public MipsBlock(BasicBlock block) {
        this(block, ".L" + counter.get());
    }

    public MipsBlock(BasicBlock block, String name) {
        this.name = name;
        if (block != null) {
            comment.append("%" + block.getName()).append(": ").append(block.comment.getComment());
        }
    }

    public DualLinkedList<MipsInstruction> getInstructions() {
        return instructions;
    }

    public static void addEdge(MipsBlock from, MipsBlock to) {
        from.successors.add(to);
        to.predecessors.add(from);
    }

    public static void removeEdge(MipsBlock from, MipsBlock to) {
        from.successors.remove(to);
        to.predecessors.remove(from);
    }

    public Set<MipsBlock> getSuccessors() {
        return successors;
    }

    public Set<MipsBlock> getPredecessors() {
        return predecessors;
    }

    public void addInstructionFirst(MipsInstruction instruction) {
        new DualLinkedList.Node<>(instruction).insertIntoHead(instructions);
        if (reservedComment != null) {
            instruction.comment.append(reservedComment);
            reservedComment = null;
        }
    }

    public void addInstruction(MipsInstruction instruction) {
        new DualLinkedList.Node<>(instruction).insertIntoTail(instructions);
        if (reservedComment != null) {
            instruction.comment.append(reservedComment);
            reservedComment = null;
        }
    }

    public void addInstructionAfter(MipsInstruction instruction, Predicate<MipsInstruction> predicate) {
        for (DualLinkedList.Node<MipsInstruction> node : instructions) {
            if (predicate.test(node.getData())) {
                new DualLinkedList.Node<>(instruction).insertAfter(node);
                return;
            }
        }
        throw new RuntimeException("Cannot find the instruction to insert after");
    }

    @Override
    public String toString() {
        return name;
    }

    public String toMips() {
        StringBuilder sb = new StringBuilder();
        sb.append("    ").append(comment).append("\n");
        sb.append("    # Predecessors: ").append(predecessors).append("\n");
        sb.append("    # Successors:   ").append(successors).append("\n");
        sb.append("  ").append(name).append(":").append("\n");
        if (isMainExit()) {
            sb.append("#### exit ####\n");
            sb.append("    li $v0, 10\n");
            sb.append("    syscall\n");
            sb.append("#### origin ####\n");
        }
        for (DualLinkedList.Node<MipsInstruction> node : instructions) {
            sb.append(node.getData()).append("\n");
        }
        return sb.toString();
    }

    public boolean isMainExit() {
        return name.equals("main.exit");
    }

    public boolean isMainEntry() {
        return name.equals("main.entry");
    }
}
