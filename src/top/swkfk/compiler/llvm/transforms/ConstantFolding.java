package top.swkfk.compiler.llvm.transforms;

import top.swkfk.compiler.llvm.IrModule;
import top.swkfk.compiler.llvm.Pass;
import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.llvm.value.Function;
import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.llvm.value.constants.ConstInteger;
import top.swkfk.compiler.llvm.value.instruction.BinaryOp;
import top.swkfk.compiler.llvm.value.instruction.IBinary;
import top.swkfk.compiler.llvm.value.instruction.IComparator;
import top.swkfk.compiler.llvm.value.instruction.IConvert;
import top.swkfk.compiler.utils.DualLinkedList;

import java.util.HashMap;
import java.util.List;

final public class ConstantFolding extends Pass {
    @Override
    public String getName() {
        return "constant-folding";
    }

    @Override
    public void run(IrModule module) {
        module.getFunctions().forEach(this::run);
    }

    private void run(Function function) {
        HashMap <Value, ConstInteger> map = new HashMap<>();
        boolean changed = true;
        while (changed) {
            changed = false;

            for (DualLinkedList.Node<BasicBlock> bNode : function.getBlocks()) {
                BasicBlock block = bNode.getData();
                for (DualLinkedList.Node<User> iNode : block.getInstructions()) {
                    User instruction = iNode.getData();
                    // do calculate for all constants
                    if (!map.containsKey(instruction) && (instruction instanceof IBinary || instruction instanceof IComparator)) {
                        if (Value.allConstInteger(instruction.getOperand(0), instruction.getOperand(1))) {
                            BinaryOp opcode;
                            if (instruction instanceof IBinary) {
                                opcode = ((IBinary) instruction).getOpcode();
                            } else {
                                opcode = ((IComparator) instruction).getOpcode();
                            }
                            int result = opcode.calculate(
                                ((ConstInteger) instruction.getOperand(0)).getValue(),
                                ((ConstInteger) instruction.getOperand(1)).getValue()
                            );
                            map.put(instruction, new ConstInteger(result, instruction.getType()));
                            changed = true;
                        }
                    }
                    if (!map.containsKey(instruction) && instruction instanceof IConvert convert) {
                        if (instruction.getOperand(0) instanceof ConstInteger integer) {
                            map.put(instruction, new ConstInteger(
                                (int) (integer.getValue() & ((1L << (convert.getType().sizeof() * 8)) - 1)),
                                instruction.getType()
                            ));
                            changed = true;
                        }
                    }
                    // try to replace
                    List<Value> operands = instruction.getOperands();
                    for (int i = 0; i < operands.size(); i++) {
                        Value operand = operands.get(i);
                        if (map.containsKey(operand)) {
                            instruction.replaceOperand(i, map.get(operand));
                            debug("replace " + operand + " with " + map.get(operand));
                            changed = true;
                        }
                    }
                }
            }
        }
    }
}
