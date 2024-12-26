package top.swkfk.compiler.arch.mips.operand;

/**
 * 这里有一个简单的处理，标签也作为立即数，存储的是标签的名字（字符串）
 */
final public class MipsImmediate extends MipsOperand {
    private final Object value;

    public MipsImmediate(int value) {
        this.value = value;
    }

    public MipsImmediate(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public int asInt() {
        // 显然，如果是字符串，也即标签，这里会抛出异常
        return (int) value;
    }
}
