package top.swkfk.compiler.llvm.value.instruction;

import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.utils.Either;
import top.swkfk.compiler.utils.Pair;

final public class IBranch extends ITerminator {

    public IBranch(BasicBlock target) {
        super("", null);
        addOperandNullable(target);
    }

    public IBranch(Value value, BasicBlock trueTarget, BasicBlock falseTarget) {
        super("", null);
        addOperand(value);
        addOperandNullable(trueTarget);
        addOperandNullable(falseTarget);
    }

    public boolean isConditional() {
        return operands.size() == 3;
    }

    public BasicBlock getTarget() {
        return (BasicBlock) getOperand(0);
    }

    public Pair<BasicBlock, BasicBlock> getConditionalTarget() {
        return new Pair<>((BasicBlock) getOperand(1), (BasicBlock) getOperand(2));
    }

    public void fillNullBlock(BasicBlock block) {
        if (isConditional()) {
            if (getOperand(1) == null) {
                replaceOperand(1, block);
            }
            if (getOperand(2) == null) {
                replaceOperand(2, block);
            }
        } else {
            if (getOperand(0) == null) {
                replaceOperand(0, block);
            }
        }
    }

    @Override
    public BasicBlock[] getSuccessors() {
        if (isConditional()) {
            return new BasicBlock[]{(BasicBlock) getOperand(1), (BasicBlock) getOperand(2)};
        } else {
            return new BasicBlock[]{(BasicBlock) getOperand(0)};
        }
    }

    @Override
    public String toLLVM() {
        StringBuilder sb = new StringBuilder();
        sb.append("br ");
        if (!isConditional()) {
            sb.append("label %").append(getOperand(0).getName());
        } else {
            sb.append(getOperand(0)).append(", ");
            sb.append("label %").append(getOperand(1).getName()).append(", ");
            sb.append("label %").append(getOperand(2).getName());
        }
        return sb.toString();
    }
}
