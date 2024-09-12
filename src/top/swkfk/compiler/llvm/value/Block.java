package top.swkfk.compiler.llvm.value;

import top.swkfk.compiler.helpers.GlobalCounter;

final public class Block extends Value {
    private final static GlobalCounter counter = new GlobalCounter();

    public Block() {
        super("" + Block.counter.get(), null);
    }
}
