package top.swkfk.compiler.utils;

public record Pair<T, U>(T first, U second) {

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
