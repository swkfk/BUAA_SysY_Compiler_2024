package top.swkfk.compiler.utils;

public class Pair<T, U> {
    public T first;
    public U second;

    public Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return first;
    }

    public U getSecond() {
        return second;
    }

    public void setFirst(T first) {
        this.first = first;
    }

    public void setSecond(U second) {
        this.second = second;
    }

    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    public boolean equals(Object obj) {
        if (obj instanceof Pair<?, ?> pair) {
            return first.equals(pair.first) && second.equals(pair.second);
        }
        return false;
    }

    public int hashCode() {
        return first.hashCode() ^ second.hashCode();
    }
}
