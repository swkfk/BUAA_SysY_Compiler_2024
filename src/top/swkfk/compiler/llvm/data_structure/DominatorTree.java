package top.swkfk.compiler.llvm.data_structure;

import top.swkfk.compiler.llvm.value.BasicBlock;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents the dominator tree of a function.
 *
 */
final public class DominatorTree {
    private final Map<BasicBlock, Set<BasicBlock>> dominator = new HashMap<>();

    public DominatorTree(Map<BasicBlock, Set<BasicBlock>> dominator) {
        this.dominator.putAll(dominator);
    }

    public Set<BasicBlock> getDominator(BasicBlock block) {
        return dominator.get(block);
    }
}
