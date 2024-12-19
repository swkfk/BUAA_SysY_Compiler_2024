package top.swkfk.compiler.llvm.analysises;

import top.swkfk.compiler.llvm.IrModule;
import top.swkfk.compiler.llvm.Pass;
import top.swkfk.compiler.llvm.data_structure.ControlFlowGraph;
import top.swkfk.compiler.llvm.data_structure.DominatorTree;
import top.swkfk.compiler.llvm.data_structure.LoopInformation;
import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.llvm.value.Function;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

final public class AnalyseLoop extends Pass {
    @Override
    public String getName() {
        return "loop-analysis";
    }

    @Override
    public boolean canPrintVerbose() {
        return false;
    }

    // Function-level local variables
    private final HashMap<BasicBlock, LoopInformation> loopInfoMap = new HashMap<>();
    private final List<LoopInformation> loops = new LinkedList<>();
    private final List<LoopInformation> allLoops = new LinkedList<>();

    @Override
    public void run(IrModule module) {
        module.getFunctions().forEach(function -> {
            loopInfoMap.clear();
            loops.clear();
            allLoops.clear();
            detectLoopsInFunction(function);
        });
    }

    public void detectLoopsInFunction(Function function) {
        DominatorTree dom = function.dom.get();
        ControlFlowGraph cfg = function.cfg.get();

        List<BasicBlock> postOrder = dom.getPostOrder(function.getBlocks().getHead().getData());
        debug("Post order: " + postOrder.stream().map(BasicBlock::getName).toList());

        for (var header : postOrder) {
            Stack<BasicBlock> backEdges = new Stack<>();
            for (var block : cfg.getPredecessors(header)) {
                if (dom.isAncestor(header, block)) {
                    backEdges.push(block);
                }
            }
            if (!backEdges.empty()) {
                detectLoop(header, backEdges, cfg);
            }
        }

        for (BasicBlock block : postOrder) {
            fillLoopInfo(block);
        }

        tidyUpAllLoops();

        function.loopMap.set(new HashMap<>(loopInfoMap));
        function.loops.set(new LinkedList<>(loops));
        function.allLoops.set(new LinkedList<>(allLoops));

        for (LoopInformation loop : allLoops) {
            for (BasicBlock block : loop.getBlocks()) {
                for (BasicBlock successor : cfg.getSuccessors(block)) {
                    if (!loop.getBlocks().contains(successor)) {
                        loop.addExitBlock(successor);
                        loop.addExitingBlock(block);
                    }
                }
            }
            for (BasicBlock predecessor : cfg.getPredecessors(loop.getBlocks().get(0))) {
                if (!loop.getBlocks().contains(predecessor)) {
                    loop.addLatchBlock(predecessor);
                }
            }
        }

        debug("Function " + function.getName() + "'s loops: \n" +
            allLoops.stream().map(LoopInformation::toString).collect(Collectors.joining("\n")));
    }

    /**
     * Detect loop in the function of the given header
     * @param header the header of the loop
     * @param backEdges will be empty after this function
     * @param cfg the control flow graph of the function
     */
    private void detectLoop(BasicBlock header, Stack<BasicBlock> backEdges, ControlFlowGraph cfg) {
        LoopInformation currentLoop = new LoopInformation(header);
        while (!backEdges.empty()) {
            BasicBlock backEdge = backEdges.pop();
            LoopInformation subLoop = loopInfoMap.get(backEdge);
            if (subLoop == null) {
                loopInfoMap.put(backEdge, currentLoop);
                if (backEdge == currentLoop.getHeader()) {
                    continue;
                }
                for (var predecessor : cfg.getPredecessors(backEdge)) {
                    backEdges.push(predecessor);
                }
            } else {
                while (subLoop.hasParent()) {
                    subLoop = subLoop.getParent();
                }
                if (subLoop == currentLoop) {
                    continue;
                }
                subLoop.setParent(currentLoop);
                for (BasicBlock predecessor : cfg.getPredecessors(subLoop.getHeader())) {
                    if (loopInfoMap.get(predecessor) != subLoop) {
                        backEdges.push(predecessor);
                    }
                }
            }
        }
    }

    private void fillLoopInfo(BasicBlock block) {
        LoopInformation subLoop = loopInfoMap.get(block);
        if (subLoop != null && subLoop.getHeader() == block) {
            if (subLoop.hasParent()) {
                subLoop.getParent().addSubLoop(subLoop);
            } else {
                loops.add(subLoop);
            }
            subLoop.reverseBlocks();
            subLoop.reverseSubLoops();
            subLoop = subLoop.getParent();
        }
        while (subLoop != null) {
            subLoop.addBlock(block);
            subLoop = subLoop.getParent();
        }
    }

    private void tidyUpAllLoops() {
        Stack<LoopInformation> queue = new Stack<>();
        queue.addAll(loops);
        allLoops.addAll(loops);
        while (!queue.isEmpty()) {
            var loop = queue.pop();
            if (!loop.getSubLoops().isEmpty()) {
                queue.addAll(loop.getSubLoops());
                allLoops.addAll(loop.getSubLoops());
            }
        }
    }
}
