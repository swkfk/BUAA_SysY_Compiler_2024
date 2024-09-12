package top.swkfk.compiler.llvm.value;

import top.swkfk.compiler.helpers.GlobalCounter;
import top.swkfk.compiler.utils.DualLinkedList;

final public class Block extends Value {
    private final static GlobalCounter counter = new GlobalCounter();

    private final DualLinkedList<User> instructions;

    public Block() {
        super("" + Block.counter.get(), null);
        this.instructions = new DualLinkedList<>();
    }
}
