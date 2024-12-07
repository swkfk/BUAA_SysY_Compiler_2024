package top.swkfk.compiler.llvm.analysises;

import top.swkfk.compiler.llvm.IrModule;
import top.swkfk.compiler.llvm.Pass;
import top.swkfk.compiler.llvm.data_structure.ControlFlowGraph;
import top.swkfk.compiler.llvm.data_structure.DominatorTree;
import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.llvm.value.Function;
import top.swkfk.compiler.utils.DualLinkedList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Use the algorithm of <a href="https://www.cs.au.dk/~gerth/advising/thesis/henrik-knakkegaard-christensen.pdf">
 *     Page 25, Algorithm 3</a>, the algorithm is as follows:
 * <pre>
 *  for w in V do
 *      traverse V', V' <- V - {w}
 *      w dominates vertices not visited
 * end for
 *
 * For each vertex w in V, traverse the graph starting from *the root*.
 * </pre>
 *
 */
final public class AnalyseDominatorTree extends Pass {
    @Override
    public boolean canPrintVerbose() {
        return false;
    }

    @Override
    public String getName() {
        return "dom-tree";
    }

    @Override
    public void run(IrModule module) {
        module.getFunctions().forEach(function ->
            function.dom.set(analyse(function))
        );
        debug("finished");
    }

    public DominatorTree analyse(Function function) {
        BasicBlock entry = function.getBlocks().getHead().getData();
        Map<BasicBlock, Set<BasicBlock>> dominator = new HashMap<>();
        for (DualLinkedList.Node<BasicBlock> bNode : function.getBlocks()) {
            BasicBlock block = bNode.getData();

            // Remove this block and traverse the graph
            Set<BasicBlock> visited = new HashSet<>() {{
                add(block);
            }};
            dfs(entry, function.cfg.get(), visited);

            // Add the dominator tree edge
            for (DualLinkedList.Node<BasicBlock> sonNode : function.getBlocks()) {
                BasicBlock sonBlock = sonNode.getData();
                if (!visited.contains(sonBlock)) {
                    dominator.computeIfAbsent(sonBlock, k -> new HashSet<>()).add(block);
                }
            }
        }
        // Copied in the constructor of DominatorTree
        return new DominatorTree(dominator);
    }

    private void dfs(BasicBlock block, ControlFlowGraph cfg, Set<BasicBlock> visited) {
        if (visited.contains(block)) {
            return;
        }
        visited.add(block);
        for (BasicBlock son : cfg.getSuccessors(block)) {
            dfs(son, cfg, visited);
        }
    }
}
