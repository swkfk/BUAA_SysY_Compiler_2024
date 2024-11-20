package top.swkfk.compiler.llvm.transforms;

import top.swkfk.compiler.llvm.IrModule;
import top.swkfk.compiler.llvm.Pass;

final public class Hello extends Pass {
    @Override
    public String getName() {
        return "hello-pass";
    }

    @Override
    public void run(IrModule module) {
        debug("Hello, world!");
        module.getFunctions().forEach(
            f -> f.getBlocks().getHead().getData().appendComment(" ^_^ Hello, world!")
        );
    }
}
