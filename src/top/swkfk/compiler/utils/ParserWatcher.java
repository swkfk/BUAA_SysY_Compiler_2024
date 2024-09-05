package top.swkfk.compiler.utils;

import top.swkfk.compiler.Configure;

import java.util.LinkedList;
import java.util.List;

final public class ParserWatcher {
    private final List<String> lines;

    public ParserWatcher() {
        if (Configure.debug.displayTokensWithAst) {
            lines = new LinkedList<>();
        } else {
            lines = null;
        }
    }

    public void add(String line) {
        if (lines != null) {
            lines.add(line);
        }
    }

    public String toString() {
        if (lines == null) {
            return "";
        }
        return String.join("\n", lines);
    }
}
