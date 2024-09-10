package top.swkfk.compiler.frontend.symbol;

final public class FixedValue {
    private final int value;

    public FixedValue(int value) {
        this.value = value;
    }

    public FixedValue() {
        this(0);
    }

    public int getValue() {
        return value;
    }
}
