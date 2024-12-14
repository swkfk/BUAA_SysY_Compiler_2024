package top.swkfk.compiler.llvm.transforms;

import top.swkfk.compiler.llvm.IrModule;
import top.swkfk.compiler.llvm.Pass;
import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.utils.DualLinkedList;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

final public class LocalVariableNumbering extends Pass {
    @Override
    public String getName() {
        return "local-variable-numbering";
    }

    @Override
    public void run(IrModule module) {
        for (var function : module.getFunctions()) {
            for (var block : function.getBlocks()) {
                run(block.getData());
            }
        }
    }

    private void run(BasicBlock block) {
        HashMap<Integer, List<User>> map = new HashMap<>();
        HashMap<Value, Value> replaceMap = new HashMap<>();
        for (DualLinkedList.Node<User> iNode : block.getInstructions()) {
            User instruction = iNode.getData();
            Integer number = instruction.numbering();
            if (number != null) {
                if (map.containsKey(number)) {
                    for (var other : map.get(number)) {
                        if (instruction.numberingEquals(other)) {
                            replaceMap.put(instruction, other);
                            break;
                        }
                    }
                }
                map.computeIfAbsent(number, k -> new LinkedList<>()).add(instruction);
            }
            for (int i = 0; i < instruction.getOperands().size(); i++) {
                Value operand = instruction.getOperand(i);
                if (replaceMap.containsKey(operand)) {
                    instruction.replaceOperand(i, replaceMap.get(operand));
                }
            }
        }
    }
}
