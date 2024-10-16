package top.swkfk.compiler.error;

import top.swkfk.compiler.frontend.Navigation;
import top.swkfk.compiler.utils.BackTrace;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

final public class ErrorTable implements BackTrace.Traceable {
    private final List<ErrorEntry> errors;

    public ErrorTable() {
        this.errors = new LinkedList<>();
    }

    public void add(ErrorType type, Navigation navigation) {
        errors.add(new ErrorEntry(type, navigation));
    }

    public boolean noError() {
        return errors.isEmpty();
    }

    public String toString() {
        return errors.stream().sorted(Comparator.comparingInt(t0 -> Integer.parseInt(t0.toString().split(" ")[0])))
            .map(ErrorEntry::toString).collect(Collectors.joining("\n"));
    }

    public String toDebugString() {
        return errors.stream().map(ErrorEntry::toDebugString).collect(Collectors.joining("\n"));
    }

    @Override
    public Object save() {
        return errors.size();
    }

    @Override
    public void restore(Object state) {
        int size = (int) state;
        while (errors.size() > size) {
            errors.remove(errors.size() - 1);
        }
    }
}
