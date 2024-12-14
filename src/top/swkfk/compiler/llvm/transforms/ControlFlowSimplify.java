package top.swkfk.compiler.llvm.transforms;

import top.swkfk.compiler.llvm.IrModule;
import top.swkfk.compiler.llvm.Pass;
import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.llvm.value.Function;
import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.llvm.value.constants.ConstInteger;
import top.swkfk.compiler.llvm.value.instruction.IBranch;
import top.swkfk.compiler.llvm.value.instruction.IPhi;
import top.swkfk.compiler.utils.DualLinkedList;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.stream.Collectors;

final public class ControlFlowSimplify extends Pass {
    @Override
    public String getName() {
        return "control-flow-simplify";
    }

    @Override
    public void run(IrModule module) {
        for (Function function : module.getFunctions()) {
            function.cfg.invalidate();
            function.dom.invalidate();

            cutConstantCondition(function);
            var predecessor = buildPredecessorMap(function);
            dropUnreachableBlocks(function, predecessor);
            var replaceMap = mergeBasicBlock(function, predecessor);
            replaceOperand(function, replaceMap);
        }
    }

    private void replaceOperand(Function function, HashMap<Value, Value> valueMap) {
        for (DualLinkedList.Node<BasicBlock> bNode : function.getBlocks()) {
            BasicBlock block = bNode.getData();
            for (DualLinkedList.Node<User> iNode : block.getInstructions()) {
                User instruction = iNode.getData();
                for (int i = 0; i < instruction.getOperands().size(); i++) {
                    Value operand = instruction.getOperand(i);
                    if (valueMap.containsKey(operand)) {
                        instruction.replaceOperand(i, valueMap.get(operand));
                    }
                }
            }
        }
    }

    private void cutConstantCondition(Function function) {
        for (DualLinkedList.Node<BasicBlock> bNode : function.getBlocks()) {
            BasicBlock block = bNode.getData();
            if (block.getLastInstruction() instanceof IBranch branch) {
                if (branch.isConditional() && branch.getOperand(0) instanceof ConstInteger integer) {
                    block.getInstructions().getTail().drop();
                    IBranch newBranch;
                    BasicBlock dropBlock;
                    if (integer.getValue() == 0) {
                        newBranch = new IBranch((BasicBlock) branch.getOperand(2));
                        dropBlock = (BasicBlock) branch.getOperand(1);
                    } else {
                        newBranch = new IBranch((BasicBlock) branch.getOperand(1));
                        dropBlock = (BasicBlock) branch.getOperand(2);
                    }
                    block.addInstruction(newBranch);
                    for (DualLinkedList.Node<User> iNode : dropBlock.getInstructions()) {
                        User instruction = iNode.getData();
                        if (instruction instanceof IPhi phi) {
                            var incoming = phi.getIncoming();
                            for (int i = 0; i < incoming.size(); i++) {
                                if (incoming.get(i).first() == block) {
                                    phi.dropIncomingIndex(i);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private HashMap<BasicBlock, HashSet<BasicBlock>> buildPredecessorMap(Function function) {
        HashMap<BasicBlock, HashSet<BasicBlock>> map = new HashMap<>();
        Deque<BasicBlock> queue = new LinkedList<>();
        queue.add(function.getBlocks().getHead().getData());
        map.put(function.getBlocks().getHead().getData(), new HashSet<>());
        while (!queue.isEmpty()) {
            BasicBlock block = queue.poll();
            for (BasicBlock successor : block.getSuccessors()) {
                if (!map.containsKey(successor)) {
                    queue.add(successor);
                }
                map.computeIfAbsent(successor, k -> new HashSet<>()).add(block);
            }
        }
        return map;
    }

    private void dropUnreachableBlocks(Function function, HashMap<BasicBlock, HashSet<BasicBlock>> predecessor) {
        HashSet<BasicBlock> dropped = new HashSet<>();
        for (DualLinkedList.Node<BasicBlock> bNode : function.getBlocks()) {
            if (!predecessor.containsKey(bNode.getData())) {
                dropped.add(bNode.getData());
                bNode.drop();
            }
        }
        for (DualLinkedList.Node<BasicBlock> bNode : function.getBlocks()) {
            BasicBlock block = bNode.getData();
            for (DualLinkedList.Node<User> iNode : block.getInstructions()) {
                User instruction = iNode.getData();
                if (instruction instanceof IPhi phi) {
                    var incoming = phi.getIncoming();
                    LinkedList<Integer> droppedIndex = new LinkedList<>();
                    for (int i = 0; i < incoming.size(); i++) {
                        if (dropped.contains(incoming.get(i).first())) {
                            droppedIndex.add(i);
                        }
                    }
                    for (int i = droppedIndex.size() - 1; i >= 0; i--) {
                        phi.dropIncomingIndex(droppedIndex.get(i));
                    }
                }
            }
        }
    }

    private HashMap<Value, Value> mergeBasicBlock(Function function, HashMap<BasicBlock, HashSet<BasicBlock>> predecessor) {
        /// Replace the key into the value for all phi nodes
        HashMap<Value, Value> valueReplaceMap = new HashMap<>();
        for (DualLinkedList.Node<BasicBlock> bNode : function.getBlocks()) {
            BasicBlock block = bNode.getData();
            if (predecessor.get(block).size() == 1) {
                BasicBlock pred = predecessor.get(block).iterator().next();
                if (pred.getSuccessors().length == 1) {
                    // Drop the branch instruction
                    pred.getInstructions().getTail().drop();
                    // Merge the two blocks (block after pred)
                    for (DualLinkedList.Node<User> iNode : block.getInstructions()) {
                        User instruction = iNode.getData();
                        if (instruction instanceof IPhi phi) {
                            Value value = phi.getOperand(1);
                            valueReplaceMap.put(phi, valueReplaceMap.getOrDefault(value, value));
                        } else {
                            pred.addInstruction(instruction);
                        }
                    }
                    pred.comment.append(" <- {" + block.getName() + "}");
                    // Drop the block
                    bNode.drop();
                    // Set the predecessor map
                    predecessor.get(pred).remove(block);
                    for (BasicBlock successor : block.getSuccessors()) {
                        predecessor.get(successor).remove(block);
                        predecessor.get(successor).add(pred);
                        for (DualLinkedList.Node<User> iNode : successor.getInstructions()) {
                            User instruction = iNode.getData();
                            if (instruction instanceof IPhi phi) {
                                phi.replaceOperand(block, pred);
                            }
                        }
                    }
                }
            }
        }
        debug("value replace map: " + valueReplaceMap.entrySet().stream().map(
            entry -> entry.getKey().getName() + " -> " + entry.getValue().getName()
        ).collect(Collectors.joining(", ")));
        return valueReplaceMap;
    }
}
