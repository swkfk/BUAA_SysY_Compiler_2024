package top.swkfk.compiler.llvm.value;

import top.swkfk.compiler.helpers.GlobalCounter;
import top.swkfk.compiler.utils.DualLinkedList;

final public class Block extends Value {
    private final static GlobalCounter counter = new GlobalCounter();

    private final Function parent;
    private final DualLinkedList<User> instructions;

    public Block(Function parent) {
        super("" + Block.counter.get(), null);
        this.instructions = new DualLinkedList<>();
        this.parent = parent;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  ").append(getName()).append(":\n");
        for (DualLinkedList.Node<User> node : instructions) {
            sb.append("    ").append(node.getData()).append("\n");
        }
        return sb.toString();
    }
}
