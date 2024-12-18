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
import top.swkfk.compiler.utils.DualLinkedList;

import java.util.HashMap;

final public class MultiplySimplify extends Pass {
    @Override
    public String getName() {
        return "multiply-simplify";
    }

    @Override
    public void run(IrModule module) {
        module.getFunctions().forEach(function -> {
            Value.counter.set(function.resumeCounter());
            run(function);
            function.saveCounter(Value.counter.get());
        });
    }

    private void run(Function function) {
        HashMap<Value, Value> replaceMap = new HashMap<>();
        for (DualLinkedList.Node<BasicBlock> bNode : function.getBlocks()) {
            for (DualLinkedList.Node<User> iNode : bNode.getData().getInstructions()) {
                User instruction = iNode.getData();
                if (!(instruction instanceof IBinary binary)) {
                    continue;
                }
                if (binary.getOpcode() == BinaryOp.MUL) {
                    int value;
                    Value operand;
                    if (binary.getOperand(0) instanceof ConstInteger integer) {
                        value = integer.getValue();
                        operand = binary.getOperand(1);
                    } else if (binary.getOperand(1) instanceof ConstInteger integer) {
                        value = integer.getValue();
                        operand = binary.getOperand(0);
                    } else {
                        continue;
                    }
                    if (value == 0) {
                        replaceMap.put(binary, ConstInteger.zero);
                    } else if (value == 1) {
                        replaceMap.put(binary, operand);
                    } else if (value == -1) {
                        User result = new IBinary(BinaryOp.SUB, ConstInteger.zero, operand);
                        new DualLinkedList.Node<>(result).insertBefore(iNode);
                        replaceMap.put(binary, result);
                    } else if (value > 0) {
                        if (Integer.bitCount(value) == 1) {
                            int shift = Integer.numberOfTrailingZeros(value);
                            User result = new IBinary(BinaryOp.SHL, operand, new ConstInteger(shift));
                            new DualLinkedList.Node<>(result).insertBefore(iNode);
                            replaceMap.put(binary, result);
                        } else if (Integer.bitCount(value) == 2) {
                            int shift1 = Integer.numberOfTrailingZeros(value);
                            int shift2 = Integer.numberOfTrailingZeros(value >> (shift1 + 1)) + shift1 + 1;
                            User result1 = new IBinary(BinaryOp.SHL, operand, new ConstInteger(shift1));
                            new DualLinkedList.Node<>(result1).insertBefore(iNode);
                            User result2 = new IBinary(BinaryOp.SHL, operand, new ConstInteger(shift2));
                            new DualLinkedList.Node<>(result2).insertBefore(iNode);
                            User result = new IBinary(BinaryOp.ADD, result1, result2);
                            new DualLinkedList.Node<>(result).insertBefore(iNode);
                            replaceMap.put(binary, result);
                        } else {
                            int subShift = checkMultiIntoSub(value);
                            int addShift = Integer.numberOfTrailingZeros(value + (1 << subShift));
                            if (subShift == -1) {
                                continue;
                            }
                            User result1 = new IBinary(BinaryOp.SHL, operand, new ConstInteger(subShift));
                            new DualLinkedList.Node<>(result1).insertBefore(iNode);
                            User result2 = new IBinary(BinaryOp.SHL, operand, new ConstInteger(addShift));
                            new DualLinkedList.Node<>(result2).insertBefore(iNode);
                            User result = new IBinary(BinaryOp.SUB, result2, result1);
                            new DualLinkedList.Node<>(result).insertBefore(iNode);
                            replaceMap.put(binary, result);
                        }
                    }
                }
            }
        }
        for (DualLinkedList.Node<BasicBlock> bNode : function.getBlocks()) {
            for (DualLinkedList.Node<User> iNode : bNode.getData().getInstructions()) {
                User instruction = iNode.getData();
                replaceMap.forEach(instruction::replaceOperand);
            }
        }
    }

    private static int checkMultiIntoSub(int value) {
        for (int i = 0; i < 32; i++) {
            if (Integer.bitCount(value + (1 << i)) == 1) {
                return i;
            }
        }
        return -1;
    }
}
