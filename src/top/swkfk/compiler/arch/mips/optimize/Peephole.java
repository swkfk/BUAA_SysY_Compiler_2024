package top.swkfk.compiler.arch.mips.optimize;

import top.swkfk.compiler.arch.mips.MipsBlock;
import top.swkfk.compiler.arch.mips.MipsFunction;
import top.swkfk.compiler.arch.mips.MipsModule;
import top.swkfk.compiler.arch.mips.instruction.MipsIBinary;
import top.swkfk.compiler.arch.mips.instruction.MipsInstruction;
import top.swkfk.compiler.arch.mips.operand.MipsImmediate;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.utils.DualLinkedList;

final public class Peephole implements MipsPass.Physical {
    @Override
    public void run(MipsModule module) {
        for (MipsFunction function : module.getFunctions()) {
            for (DualLinkedList.Node<MipsBlock> bNode : function.getBlocks()) {
                for (DualLinkedList.Node<MipsInstruction> iNode : bNode.getData().getInstructions()) {
                    if (removable(iNode.getData())) {
                        iNode.drop();
                    }
                }
            }
        }
    }

    private boolean removable(MipsInstruction instruction) {
        if (instruction instanceof MipsIBinary binary) {
            if (binary.getOperator() == MipsIBinary.X.addiu) {
                MipsOperand[] operands = binary.getOperands();
                if (operands[1] instanceof MipsImmediate imm && imm.asInt() == 0) {
                    return operands[0] == operands[2];
                }
                if (operands[2] instanceof MipsImmediate imm && imm.asInt() == 0) {
                    return operands[0] == operands[1];
                }
            }
            if (binary.getOperator() == MipsIBinary.X.addu) {
                MipsOperand[] operands = binary.getOperands();
                if (operands[1] == MipsPhysicalRegister.zero) {
                    return operands[0] == operands[2];
                }
                if (operands[2] == MipsPhysicalRegister.zero) {
                    return operands[0] == operands[1];
                }
            }
            if (binary.getOperator() == MipsIBinary.X.subu) {
                MipsOperand[] operands = binary.getOperands();
                if (operands[2] == MipsPhysicalRegister.zero) {
                    return operands[0] == operands[1];
                }
            }
        }
        return false;
    }
}
