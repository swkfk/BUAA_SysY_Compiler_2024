package top.swkfk.compiler.llvm.transforms;

import top.swkfk.compiler.llvm.IrModule;
import top.swkfk.compiler.llvm.Pass;

final public class Dummy extends Pass {
    @Override
    public String getName() {
        return "dummy";
    }

    @Override
    public void run(IrModule module) {
        debug("dummy pass is running");
    }
}
