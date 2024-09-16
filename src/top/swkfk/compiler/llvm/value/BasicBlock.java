package top.swkfk.compiler.llvm.value;

import top.swkfk.compiler.utils.DualLinkedList;

final public class BasicBlock extends Value {
    private final Function parent;
    private final DualLinkedList<User> instructions;

    public BasicBlock(Function parent) {
        super("" + Value.counter.get(), null);
        this.instructions = new DualLinkedList<>();
        this.parent = parent;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  ").append(getName()).append(":\n");
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

    public Function getParent() {
        return parent;
    }
}
