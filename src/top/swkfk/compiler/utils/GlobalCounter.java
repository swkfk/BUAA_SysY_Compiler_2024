package top.swkfk.compiler.utils;

final public class GlobalCounter {
    private int counter = 0;

    public GlobalCounter() {
    }

    public int get() {
        return ++counter;
    }
}
