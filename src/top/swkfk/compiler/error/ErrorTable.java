package top.swkfk.compiler.error;

import top.swkfk.compiler.frontend.Navigation;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

final public class ErrorTable {
    private final List<ErrorEntry> errors;

    public ErrorTable() {
        this.errors = new LinkedList<>();
    }

    public void add(ErrorType type, Navigation navigation) {
        errors.add(new ErrorEntry(type, navigation));
    }

    public String toString() {
        return errors.stream().map(ErrorEntry::toString).collect(Collectors.joining("\n"));
    }

    public String toDebugString() {
        return errors.stream().map(ErrorEntry::toDebugString).collect(Collectors.joining("\n"));
    }
}
