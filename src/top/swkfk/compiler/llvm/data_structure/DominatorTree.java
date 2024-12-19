package top.swkfk.compiler.llvm.data_structure;

import top.swkfk.compiler.llvm.value.BasicBlock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Represents the dominator tree of a function.
 *
 */
final public class DominatorTree {
    /// Key is dominated by the values
    private final Map<BasicBlock, Set<BasicBlock>> dominatedBy = new HashMap<>();
    ///  Key dominates the values
    private final Map<BasicBlock, List<BasicBlock>> dominates = new HashMap<>();
    private final Map<BasicBlock, BasicBlock> immediateDominator = new HashMap<>();

    public DominatorTree(
        Map<BasicBlock, Set<BasicBlock>> dominatedBy,
        Map<BasicBlock, BasicBlock> immediateDominator,
        Map<BasicBlock, List<BasicBlock>> dominates
    ) {
        this.dominatedBy.putAll(dominatedBy);
        this.immediateDominator.putAll(immediateDominator);
        this.dominates.putAll(dominates);
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

    public List<BasicBlock> getPostOrder(BasicBlock entry) {
        List<BasicBlock> postOrder = new LinkedList<>();
        HashSet<BasicBlock> visited = new HashSet<>();
        Stack<BasicBlock> stack = new Stack<>();
        stack.push(entry);
        while (!stack.isEmpty()) {
            BasicBlock block = stack.peek();
            if (visited.contains(block)) {
                postOrder.add(block);
                stack.pop();
                continue;
            }
            for (BasicBlock child : dominates.get(block)) {
                stack.push(child);
            }
            visited.add(block);
        }
        return postOrder;
    }
}
