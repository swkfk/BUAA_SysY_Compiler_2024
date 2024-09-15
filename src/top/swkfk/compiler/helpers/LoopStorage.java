package top.swkfk.compiler.helpers;

import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.llvm.value.instruction.IBranch;

import java.util.List;

/**
 * Store the basic information when traversing the AST to generate the ir.
 */
public record LoopStorage(BasicBlock condBlock, List<IBranch> breaks, List<IBranch> continues) {
    public void replaceBreak(BasicBlock exit) {
        breaks.forEach(branch -> branch.fillNullBlock(exit));
    }

    public void replaceContinue(BasicBlock cond) {
        continues.forEach(branch -> branch.fillNullBlock(cond));
    }
}
