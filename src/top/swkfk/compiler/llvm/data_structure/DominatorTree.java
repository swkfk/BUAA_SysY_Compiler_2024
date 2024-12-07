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

    /**
     * Check if u is an ancestor of v.
     * @param u the potential dominator
     * @param v the potential dominated
     * @return true if u is an ancestor of v
     */
    public boolean isAncestor(BasicBlock u, BasicBlock v) {
        BasicBlock runner = v;
        while (runner != null) {
            if (runner == u) {
                return true;
            }
            runner = immediateDominator.get(runner);
        }
        return false;
    }
}
