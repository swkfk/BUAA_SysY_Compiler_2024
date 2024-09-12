package top.swkfk.compiler.llvm.value.instruction;

import top.swkfk.compiler.llvm.value.Block;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.utils.Either;
import top.swkfk.compiler.utils.Pair;

final public class IBranch extends ITerminator {
    /**
     * The target block of this branch instruction.
     * <li>Left ---- Just br directly</li>
     * <li>Right ---- br i1 %cond, label %first-true, label %second-false</li>
     */
    private final Either<Block, Pair<Block, Block>> target;

    public IBranch(Block target) {
        super("", null);
        this.target = Either.left(target);
    }

    public IBranch(Value value, Block trueTarget, Block falseTarget) {
        super("", null);
        addOperand(value);
        this.target = Either.right(new Pair<>(trueTarget, falseTarget));
    }

    @Override
    public Block[] getSuccessors() {
        if (target.isLeft()) {
            return new Block[]{target.getLeft()};
        } else {
            return new Block[]{target.getRight().first(), target.getRight().second()};
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
