package top.swkfk.compiler.frontend;

import top.swkfk.compiler.utils.Pair;

/**
 * Location of the items in the source code. The real location is
 * [start, end] in the source code. The items in the Pair is the line number
 * and the column number.
 */
public record Navigation(Pair<Integer, Integer> start, Pair<Integer, Integer> end) {

    public String toString() {
        return String.format("%d:%d-%d:%d", start.first(), start.second(), end.first(), end.second());
    }
}
