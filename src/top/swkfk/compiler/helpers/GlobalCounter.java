package top.swkfk.compiler.helpers;

final public class GlobalCounter {
    private int counter = 0;

    public GlobalCounter() {
    }

    public int get() {
        return ++counter;
    }

    public void reset() {
        counter = 0;
    }
}
