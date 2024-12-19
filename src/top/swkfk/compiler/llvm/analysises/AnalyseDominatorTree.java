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
import java.util.List;
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
        Map<BasicBlock, List<BasicBlock>> dominates = new HashMap<>();
        HashMap<BasicBlock, BasicBlock> immediateDominator = new HashMap<>();

        for (DualLinkedList.Node<BasicBlock> bNode : function.getBlocks()) {
            dominatedBy.put(bNode.getData(), new HashSet<>());
            dominates.put(bNode.getData(), new LinkedList<>());
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
                    dominatedBy.get(sonBlock).add(block);
                }
            }
        }

        // Calculate the immediate dominator
        // Reference: https://gitlab.eduxiji.net/educg-group-26173-2487151/T202410006203104-3288/-/blame/main/src/pass/utils/DominatorTree.java#L27-55

        for (var Entry : dominatedBy.entrySet()) {
            BasicBlock block = Entry.getKey();
            Set<BasicBlock> dominatorSet = Entry.getValue();
            for (BasicBlock dominator : dominatorSet) {
                if (dominator == block) {
                    continue;
                }
                boolean isImmediateDominator = true;
                for (BasicBlock other : dominatorSet) {
                    if (other != block && other != dominator && dominatedBy.get(other).contains(dominator)) {
                        isImmediateDominator = false;
                        break;
                    }
                }
                if (isImmediateDominator) {
                    immediateDominator.put(block, dominator);
                    dominates.get(dominator).add(block);
                    break;
                }
            }
        }

        debug("DominatorTree for `" + function.getName() + "`:");

        debug("DominatedBy:" + dominatedBy.keySet().stream().map(
            k -> k.getName() + " -> <" + dominatedBy.get(k).stream().map(BasicBlock::getName).collect(Collectors.joining(", ")) + ">"
        ).collect(Collectors.joining(", ")));

        debug("Dominates:" + dominates.keySet().stream().map(
            k -> k.getName() + " -> <" + dominates.get(k).stream().map(BasicBlock::getName).collect(Collectors.joining(", ")) + ">"
        ).collect(Collectors.joining(", ")));

        debug("IDom: " + immediateDominator.keySet().stream().map(
            k -> k.getName() + " -> " + immediateDominator.get(k).getName()
        ).collect(Collectors.joining(", ")));

        // Copied in the constructor of DominatorTree
        return new DominatorTree(dominatedBy, immediateDominator, dominates);
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
