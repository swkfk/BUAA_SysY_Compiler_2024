package top.swkfk.compiler.llvm.analysises;

import top.swkfk.compiler.llvm.IrModule;
import top.swkfk.compiler.llvm.Pass;
import top.swkfk.compiler.llvm.data_structure.ControlFlowGraph;
import top.swkfk.compiler.llvm.data_structure.DominatorTree;
import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.llvm.value.Function;
import top.swkfk.compiler.utils.DualLinkedList;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Use the algorithm of <a href="https://www.cs.au.dk/~gerth/advising/thesis/henrik-knakkegaard-christensen.pdf">
 *     Page 25, Algorithm 3</a> to get the dominator relationship, the algorithm is as follows:
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
    }

    public DominatorTree analyse(Function function) {
        BasicBlock entry = function.getBlocks().getHead().getData();
        Map<BasicBlock, Set<BasicBlock>> dominatedBy = new HashMap<>();
        HashMap<BasicBlock, BasicBlock> immediateDominator = new HashMap<>();

        for (DualLinkedList.Node<BasicBlock> bNode : function.getBlocks()) {
            dominatedBy.put(bNode.getData(), new HashSet<>());
        }

        // Calculate the dominator tree
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
                    dominatedBy.get(block).add(sonBlock);
                }
            }
        }

        // Calculate the immediate dominator
        // Reference: https://gitlab.eduxiji.net/educg-group-26173-2487151/T202410006203104-3288/-/blame/stable/src/pass/utils/DominatorTree.java#L25-47
        // whose author is the same as the author of this file
        Deque<BasicBlock> queue = new LinkedList<>();
        HashMap<BasicBlock, Integer> depthMap = new HashMap<>();
        HashMap<BasicBlock, Integer> visited = new HashMap<>();
        queue.add(entry);
        depthMap.put(entry, 0);
        while (!queue.isEmpty()) {
            BasicBlock block = queue.poll();
            if (visited.getOrDefault(block, -1) >= depthMap.get(block)) {
                continue;
            }
            visited.put(block, depthMap.get(block));

            int curDepth = depthMap.get(block) + 1;
            for (BasicBlock child : dominatedBy.get(block)) {
                Integer d = depthMap.getOrDefault(child, 0);
                if (d < curDepth) {
                    immediateDominator.put(child, block);
                    depthMap.put(child, curDepth);
                }
                queue.add(child);
            }
        }

        debug("DominatorTree for `" + function.getName() + "`:");

        debug("DominatedBy:" + dominatedBy.keySet().stream().map(
            k -> k.getName() + " -> <" + dominatedBy.get(k).stream().map(BasicBlock::getName).collect(Collectors.joining(", ")) + ">"
        ).collect(Collectors.joining(", ")));

        debug("IDom: " + immediateDominator.keySet().stream().map(
            k -> k.getName() + " -> " + immediateDominator.get(k).getName()
        ).collect(Collectors.joining(", ")));

        // Copied in the constructor of DominatorTree
        return new DominatorTree(dominatedBy, immediateDominator);
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
