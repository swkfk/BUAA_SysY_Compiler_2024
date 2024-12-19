package top.swkfk.compiler.llvm.transforms;

import top.swkfk.compiler.llvm.IrModule;
import top.swkfk.compiler.llvm.Pass;

public class LoopHoist extends Pass {
    @Override
    public String getName() {
        return "loop-hoist";
    }

    @Override
    public void run(IrModule module) {

    }
}
