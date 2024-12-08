package top.swkfk.compiler.helpers;

final public class GlobalCounter {
    private int counter = 0;

    public GlobalCounter() {
    }

    public int get() {
        return counter++;
    }

    public int reset() {
        int temp = counter;
        counter = 0;
        return temp;
    }

    public void set(int value) {
        counter = value;
    }
}
