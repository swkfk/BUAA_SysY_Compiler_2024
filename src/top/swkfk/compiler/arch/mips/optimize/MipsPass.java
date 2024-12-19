package top.swkfk.compiler.arch.mips.optimize;

import top.swkfk.compiler.arch.mips.MipsModule;

/**
 * Have no time to maintain the basic information of it.
 */
public interface MipsPass {
    void run(MipsModule module);

    interface Virtual extends MipsPass {
    }

    interface Physical extends MipsPass {
    }
}
