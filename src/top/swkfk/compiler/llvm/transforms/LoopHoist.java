package top.swkfk.compiler.llvm.transforms;

import top.swkfk.compiler.llvm.IrModule;
import top.swkfk.compiler.llvm.Pass;
import top.swkfk.compiler.llvm.data_structure.LoopInformation;
import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.llvm.value.Function;
import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.llvm.value.instruction.IAllocate;
import top.swkfk.compiler.llvm.value.instruction.IBranch;
import top.swkfk.compiler.llvm.value.instruction.ICall;
import top.swkfk.compiler.llvm.value.instruction.ILoad;
import top.swkfk.compiler.llvm.value.instruction.IPhi;
import top.swkfk.compiler.llvm.value.instruction.IStore;
import top.swkfk.compiler.llvm.value.instruction.ITerminator;
import top.swkfk.compiler.utils.DualLinkedList;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Loop hoisting optimization.
 * Direct reference: <a href="https://gitlab.eduxiji.net/educg-group-26173-2487151/T202410006203104-3288/-/blame/main/src/pass/ir/LoopHoist.java">compiler2024-x</a>
 * Whose author is the same as the author of this project.
 */
final public class LoopHoist extends Pass {
    // Function-level local variables
    private final List<LoopInformation> loopsToHoist = new LinkedList<>();

    @Override
    public String getName() {
        return "loop-hoist";
    }

    @Override
    public void run(IrModule module) {
        for (Function function : module.getFunctions()) {
            loopsToHoist.clear();
            Value.counter.set(function.resumeCounter());
            run(function);
            function.saveCounter(Value.counter.reset());
            function.dom.invalidate();
            function.cfg.invalidate();
            function.loops.invalidate();
            function.allLoops.invalidate();
            function.loopMap.invalidate();
        }
    }

    private void run(Function function) {
        BasicBlock currentHoistedBlock;

        // 1. Get all loops in order. Inner loops come first.
        for (LoopInformation topLoop : function.loops.get()) {
            visitLoop(topLoop);
            loopsToHoist.add(topLoop);
        }

        // 2. Hoist all loops.
        for (var loop : loopsToHoist) {
            // 2.1 Create a new block before the loop header.
            currentHoistedBlock = new BasicBlock(function, loop.getHeader().getName() + "_hoisted");
            for (var predecessor : new LinkedList<>(function.cfg.get().getPredecessors(loop.getHeader()))) {
                if (!loop.getLatch().contains(predecessor)) {
                    continue;
                }
                // Modify the jump target
                Objects.requireNonNull(predecessor.getLastInstruction())
                    .replaceOperand(loop.getHeader(), currentHoistedBlock);
                // Modify the CFG
                function.cfg.get().insertPredecessor(loop.getHeader(), predecessor, currentHoistedBlock);
                // Insert in the loop
                LoopInformation visitedLoop = loop.getParent();
                while (visitedLoop != null) {
                    visitedLoop.insertBefore(loop.getHeader(), currentHoistedBlock);
                    visitedLoop = visitedLoop.getParent();
                }
                // Modify the phi nodes sources
                for (DualLinkedList.Node<User> iNode : loop.getHeader().getInstructions()) {
                    User instruction = iNode.getData();
                    if (instruction instanceof IPhi phi) {
                        phi.replaceOperand(predecessor, currentHoistedBlock);
                    } else {
                        break;
                    }
                }
            }
            // 2.2 Check all expressions to filter those hoist-able.
            LinkedHashSet<User> marked = new LinkedHashSet<>();
            HashSet<Value> valuePointInner = new HashSet<>();
            for (BasicBlock block : loop.getBlocks()) {
                for (DualLinkedList.Node<User> iNode : block.getInstructions()) {
                    User instruction = iNode.getData();
                    valuePointInner.add(instruction);
                }
            }
            boolean changed = true;
            while (changed) {
                changed = false;
                for (BasicBlock block : loop.getBlocks()) {
                    var iter = block.getInstructions().iterator();
                    while (iter.hasNext()) {
                        var instruction = iter.next().getData();
                        if (instruction instanceof ITerminator || instruction instanceof IAllocate || instruction instanceof ILoad
                            || instruction instanceof IPhi || instruction instanceof IStore || instruction instanceof ICall) {
                            continue;
                        }
                        boolean allInvariant = true;
                        for (var operand : instruction.getOperands()) {
                            if (valuePointInner.contains(operand)) {
                                allInvariant = false;
                                break;
                            }
                        }
                        if (allInvariant && !marked.contains(instruction)) {
                            // 2.3 Move hoist-able expressions to the new block.
                            marked.add(instruction);
                            iter.remove();

                            debug("Move " + instruction + " to " + currentHoistedBlock.getName());
                            new DualLinkedList.Node<>(instruction).insertIntoTail(currentHoistedBlock.getInstructions());

                            valuePointInner.remove(instruction);

                            changed = true;
                        }
                    }
                }
            }
            // 2.4 Add the new block to the function.
            new DualLinkedList.Node<User>(new IBranch(loop.getHeader())).insertIntoTail(currentHoistedBlock.getInstructions());
            function.addBlock(currentHoistedBlock);
        }
    }

    private void visitLoop(LoopInformation loop) {
        for (var subLoop : loop.getSubLoops()) {
            visitLoop(subLoop);
            loopsToHoist.add(subLoop);
        }
    }
}
