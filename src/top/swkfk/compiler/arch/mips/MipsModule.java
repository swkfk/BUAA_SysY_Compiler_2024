package top.swkfk.compiler.arch.mips;

import top.swkfk.compiler.Configure;
import top.swkfk.compiler.arch.ArchModule;
import top.swkfk.compiler.llvm.IrModule;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MipsModule implements ArchModule {
    @Override
    public ArchModule runParseIr(IrModule module) {
        return this;
    }

    @Override
    public ArchModule runRemovePhi() {
        return this;
    }

    @Override
    public ArchModule runVirtualOptimize() {
        return this;
    }

    @Override
    public ArchModule runRegisterAllocation() {
        return this;
    }

    @Override
    public ArchModule runPhysicalOptimize() {
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Module: ").append(Configure.source).append("\n");

        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        sb.append("# Compiled at: ").append(time).append("\n");

        //

        return sb.toString();
    }
}
