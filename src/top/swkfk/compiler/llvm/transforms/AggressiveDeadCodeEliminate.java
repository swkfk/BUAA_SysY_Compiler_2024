package top.swkfk.compiler.llvm.transforms;

import top.swkfk.compiler.llvm.IrModule;
import top.swkfk.compiler.llvm.Pass;
import top.swkfk.compiler.llvm.Use;
import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.llvm.value.Function;
import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.llvm.value.instruction.IBranch;
import top.swkfk.compiler.llvm.value.instruction.ICall;
import top.swkfk.compiler.llvm.value.instruction.IPhi;
import top.swkfk.compiler.llvm.value.instruction.IReturn;
import top.swkfk.compiler.llvm.value.instruction.IStore;
import top.swkfk.compiler.llvm.value.instruction.ITerminator;
import top.swkfk.compiler.utils.DualLinkedList;

import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

final public class AggressiveDeadCodeEliminate extends Pass {
    @Override
    public String getName() {
        return "aggressive-dead-code-eliminate";
    }

    @Override
    public void run(IrModule module) {
        module.getFunctions().forEach(this::run);
    }

    private void run(Function function) {
        HashSet<User> liveInstructions = new HashSet<>();
        HashSet<BasicBlock> liveBlocks = new HashSet<>();
        Deque<User> workList = new LinkedList<>();
        HashMap<Value, BasicBlock> blockMap = new HashMap<>();

        for (DualLinkedList.Node<BasicBlock> bNode : function.getBlocks()) {
            BasicBlock block = bNode.getData();
            for (DualLinkedList.Node<User> iNode : block.getInstructions()) {
                User instruction = iNode.getData();
                blockMap.put(instruction, block);
                if (instruction instanceof IStore || instruction instanceof ICall || instruction instanceof IReturn) {
                    if (!workList.contains(instruction)) {
                        workList.add(instruction);
                    }
                }
            }
        }

        while (!workList.isEmpty()) {
            User instruction = workList.pop();
            BasicBlock currentBlock = blockMap.get(instruction);
            liveInstructions.add(instruction);
            liveBlocks.add(currentBlock);

            if (instruction instanceof IPhi phi) {
                for (var incoming : phi.getIncoming()) {
                    BasicBlock incomingBlock = incoming.first();
                    if (!liveBlocks.contains(incomingBlock)) {
                        workList.add(incomingBlock.getLastInstruction());
                    }
                    liveBlocks.add(incomingBlock);
                }
            }

            for (BasicBlock predecessor : function.cfg.get().getPredecessors(currentBlock)) {
                if (!liveInstructions.contains(predecessor.getLastInstruction())) {
                    workList.add(predecessor.getLastInstruction());
                }
            }

            for (Use use : instruction.getUses()) {
                Value usedValue = use.getValue();
                if (!(usedValue instanceof User)) {
                    continue;
                }
                if (!liveInstructions.contains(usedValue)) {
                    workList.add((User) usedValue);
                }
            }
        }

        List<BasicBlock> deadBlocks = new LinkedList<>();

        for (DualLinkedList.Node<BasicBlock> bNode : function.getBlocks()) {
            BasicBlock block = bNode.getData();
            if (!liveBlocks.contains(block)) {
                debug("Not live block: " + block.getName());
            }
            var iter = block.getInstructions().iterator();
            while (iter.hasNext()) {
                User instruction = iter.next().getData();
                if (!liveInstructions.contains(instruction)) {
                    debug("Remove dead code: " + instruction.toLLVM());
                    iter.remove();
                }
            }
            if (block.getInstructions().isEmpty()) {
                bNode.drop();
                deadBlocks.add(block);
            }
        }

        if (!deadBlocks.isEmpty()) {
            debug("Remove dead blocks: " + deadBlocks.stream().map(BasicBlock::getName).toList());
        }

        for (DualLinkedList.Node<BasicBlock> bNode : function.getBlocks()) {
            BasicBlock block = bNode.getData();
            ITerminator terminator = (ITerminator) block.getLastInstruction();
            assert terminator != null : "The last instruction of a basic block must be a terminator and not null";
            if (Arrays.stream(terminator.getSuccessors()).anyMatch(deadBlocks::contains)) {
                debug("Replace dead terminator: " + block.getLastInstruction().toLLVM());
                if (terminator.getSuccessors().length != 2) {
                    throw new RuntimeException("Only two successors can be replaced");
                }
                BasicBlock successor = Arrays.stream(terminator.getSuccessors())
                    .filter(Objects::nonNull)
                    .filter(b -> !deadBlocks.contains(b))
                    .findFirst()
                    .orElseThrow();
                block.getInstructions().getTail().drop();
                block.addInstruction(new IBranch(successor));
            }
        }
    }
}
