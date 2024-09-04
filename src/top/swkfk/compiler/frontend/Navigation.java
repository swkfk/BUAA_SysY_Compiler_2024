package top.swkfk.compiler.frontend;

import top.swkfk.compiler.utils.Pair;

/**
 * Location of the items in the source code. The real location is
 * [start, end] in the source code. The items in the Pair is the line number
 * and the column number.
 */
public class Navigation {
    private Pair<Integer, Integer> start;
    private Pair<Integer, Integer> end;

    public Navigation(Pair<Integer, Integer> start, Pair<Integer, Integer> end) {
        this.start = start;
        this.end = end;
    }

    public Navigation(Pair<Integer, Integer> start) {
        this.start = start;
        this.end = new Pair<>(-1, -1);
    }

    public void setStart(Pair<Integer, Integer> start) {
        this.start = start;
    }

    public void setStart(Navigation start) {
        this.start = start.start;
    }

    public void setEnd(Pair<Integer, Integer> end) {
        this.end = end;
    }

    public void setEnd(Navigation end) {
        this.end = end.end;
    }

    public Pair<Integer, Integer> getStart() {
        return start;
    }

    public Pair<Integer, Integer> getEnd() {
        return end;
    }

    public String toString() {
        return String.format("%d:%d-%d:%d", start.getFirst(), start.getSecond(), end.getFirst(), end.getSecond());
    }
}
