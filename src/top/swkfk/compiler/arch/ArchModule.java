package top.swkfk.compiler.arch;

import top.swkfk.compiler.llvm.IrModule;

public interface ArchModule {
    /**
     * Parse IR to architecture module. Do instruction selection.
     * @param module IR module
     * @return Architecture module (Self)
     */
    ArchModule runParseIr(IrModule module);

    /**
     * Run remove phi optimization.
     * @return Architecture module (Self)
     */
    ArchModule runRemovePhi();

    /**
     * Run optimization based on virtual registers.
     * @return Architecture module (Self)
     */
    ArchModule runVirtualOptimize();

    /**
     * Run register allocation.
     * @return Architecture module (Self)
     */
    ArchModule runRegisterAllocation();

    /**
     * Run optimization based on physical registers.
     * @return Architecture module (Self)
     */
    ArchModule runPhysicalOptimize();
}
