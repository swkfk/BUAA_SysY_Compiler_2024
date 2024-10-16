package top.swkfk.compiler.helpers;

import top.swkfk.compiler.Configure;
import top.swkfk.compiler.utils.BackTrace;

import java.util.LinkedList;
import java.util.List;

final public class ParserWatcher implements BackTrace.Traceable {
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

    @Override
    public Object save() {
        return lines == null ? null : lines.size();
    }

    @Override
    public void restore(Object state) {
        if (state == null || lines == null) {
            return;
        }
        while (lines.size() > (int) state) {
            lines.remove(lines.size() - 1);
        }
    }
}
