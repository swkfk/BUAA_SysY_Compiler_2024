package top.swkfk.compiler.arch.mips.operand;

final public class MipsImmediate extends MipsOperand {
    private final int value;

    public MipsImmediate(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
