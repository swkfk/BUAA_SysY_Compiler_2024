package top.swkfk.compiler.llvm.data_structure;

import top.swkfk.compiler.llvm.value.BasicBlock;

import java.util.Map;
import java.util.Set;

/**
 * Represents the control flow graph of a function.
 */
final public class ControlFlowGraph {
    private final BasicBlock entry;
    private final Map<BasicBlock, Set<BasicBlock>> successors;
    private final Map<BasicBlock, Set<BasicBlock>> predecessors;
    private final Set<BasicBlock> blocks;

    public ControlFlowGraph(BasicBlock entry, Map<BasicBlock, Set<BasicBlock>> successors, Map<BasicBlock, Set<BasicBlock>> predecessors, Set<BasicBlock> visited) {
        this.entry = entry;
        this.successors = successors;
        this.predecessors = predecessors;
        this.blocks = visited;
    }

    public BasicBlock getEntry() {
        return entry;
    }

    public Set<BasicBlock> getSuccessors(BasicBlock block) {
        return successors.get(block);
    }

    public Set<BasicBlock> getPredecessors(BasicBlock block) {
        return predecessors.get(block);
    }

    public boolean contains(BasicBlock block) {
        return blocks.contains(block);
    }
}
