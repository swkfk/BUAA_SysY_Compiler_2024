package top.swkfk.compiler.llvm.transforms;

import top.swkfk.compiler.llvm.IrModule;
import top.swkfk.compiler.llvm.Pass;

public class DeadBlockEliminate extends Pass {
    @Override
    public String getName() {
        return "dead-block-eliminate";
    }

    @Override
    public void run(IrModule module) {
        module.getFunctions().forEach(function -> function.getBlocks().forEach(block -> {
            if (!function.cfg.get().contains(block.getData())) {
                block.drop();
            }
        }));
    }
}
