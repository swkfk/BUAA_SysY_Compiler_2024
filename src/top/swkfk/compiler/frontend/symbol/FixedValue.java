package top.swkfk.compiler.frontend.symbol;

final public class FixedValue {
    private final int value;

    public FixedValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
