package top.swkfk.compiler.llvm.analysises;

import top.swkfk.compiler.llvm.IrModule;
import top.swkfk.compiler.llvm.Pass;
import top.swkfk.compiler.llvm.data_structure.ControlFlowGraph;
import top.swkfk.compiler.llvm.value.BasicBlock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

final public class AnalyseControlFlowGraph extends Pass {
    @Override
    public boolean canPrintVerbose() {
        return false;
    }

    @Override
    public String getName() {
        return "control-flow-graph";
    }

    @Override
    public void run(IrModule module) {
        module.getFunctions().forEach(function ->
            function.cfg.set(analyse(function.getBlocks().getHead().getData()))
        );
    }

    private ControlFlowGraph analyse(BasicBlock entry) {
        Map<BasicBlock, Set<BasicBlock>> successors = new HashMap<>();
        Map<BasicBlock, Set<BasicBlock>> predecessors = new HashMap<>();
        Set<BasicBlock> visited = new HashSet<>();
        Queue<BasicBlock> queue = new LinkedList<>();
        queue.add(entry);

        while (!queue.isEmpty()) {
            BasicBlock block = queue.poll();
            if (visited.contains(block)) {
                continue;
            }
            visited.add(block);

            predecessors.putIfAbsent(block, new HashSet<>());
            successors.put(block, new HashSet<>());
            for (BasicBlock successor : block.getSuccessors()) {
                successors.get(block).add(successor);
                predecessors.computeIfAbsent(successor, k -> new HashSet<>()).add(block);
                queue.add(successor);
            }
        }

        return new ControlFlowGraph(entry, successors, predecessors, visited);
    }
}
