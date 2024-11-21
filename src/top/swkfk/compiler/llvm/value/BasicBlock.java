package top.swkfk.compiler.llvm.value;

import top.swkfk.compiler.helpers.Comments;
import top.swkfk.compiler.utils.DualLinkedList;

final public class BasicBlock extends Value {
    private final Function parent;
    private final DualLinkedList<User> instructions;
    public final Comments comment = new Comments("\t\t\t; ");

    public BasicBlock(Function parent, String comment) {
        super("" + Value.counter.get(), null);
        this.instructions = new DualLinkedList<>();
        this.parent = parent;
        this.comment.append(comment);
    }

    public DualLinkedList<User> getInstructions() {
        return instructions;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  ").append(getName()).append(":");
        sb.append(comment).append("\n");
        for (DualLinkedList.Node<User> node : instructions) {
            sb.append("    ").append(node.getData().toLLVM()).append("\n");
        }
        return sb.toString();
    }

    public void addInstruction(User instruction) {
        new DualLinkedList.Node<>(instruction).insertIntoTail(instructions);
    }

    public User getLastInstruction() {
        return instructions.isEmpty() ? null :
            instructions.getTail().getData();
    }

    public void removeLastInstruction() {
        instructions.getTail().drop();
    }

    public Function getParent() {
        return parent;
    }
}
