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
    private final Map<BasicBlock, Set<BasicBlock>> dominatedBy = new HashMap<>();
    private final Map<BasicBlock, BasicBlock> immediateDominator = new HashMap<>();

    public DominatorTree(Map<BasicBlock, Set<BasicBlock>> dominatedBy, Map<BasicBlock, BasicBlock> immediateDominator) {
        this.dominatedBy.putAll(dominatedBy);
        this.immediateDominator.putAll(immediateDominator);
    }

    public Set<BasicBlock> getDominator(BasicBlock block) {
        return dominatedBy.get(block);
    }

    public BasicBlock getImmediateDominator(BasicBlock block) {
        return immediateDominator.get(block);
    }
}
