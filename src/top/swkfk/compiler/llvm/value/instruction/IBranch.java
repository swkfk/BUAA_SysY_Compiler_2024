package top.swkfk.compiler.llvm.value.instruction;

import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.utils.Either;
import top.swkfk.compiler.utils.Pair;

final public class IBranch extends ITerminator {
    /**
     * The target block of this branch instruction.
     * <li>Left ---- Just br directly</li>
     * <li>Right ---- br i1 %cond, label %first-true, label %second-false</li>
     */
    private final Either<BasicBlock, Pair<BasicBlock, BasicBlock>> target;

    public IBranch(BasicBlock target) {
        super("", null);
        this.target = Either.left(target);
    }

    public IBranch(Value value, BasicBlock trueTarget, BasicBlock falseTarget) {
        super("", null);
        addOperand(value);
        this.target = Either.right(new Pair<>(trueTarget, falseTarget));
    }

    @Override
    public BasicBlock[] getSuccessors() {
        if (target.isLeft()) {
            return new BasicBlock[]{target.getLeft()};
        } else {
            return new BasicBlock[]{target.getRight().first(), target.getRight().second()};
        }
    }

    @Override
    public String toLLVM() {
        StringBuilder sb = new StringBuilder();
        sb.append("br ");
        if (target.isLeft()) {
            sb.append("label %").append(target.getLeft().getName());
        } else {
            sb.append(getOperand(0)).append(", ");
            sb.append("label %").append(target.getRight().first().getName()).append(", ");
            sb.append("label %").append(target.getRight().second().getName());
        }
        return sb.toString();
    }
}
