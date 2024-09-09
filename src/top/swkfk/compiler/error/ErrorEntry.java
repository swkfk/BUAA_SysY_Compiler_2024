package top.swkfk.compiler.error;

import top.swkfk.compiler.frontend.Navigation;

final public class ErrorEntry {
    private final ErrorType type;
    private final Navigation navigation;

    public ErrorEntry(ErrorType type, Navigation navigation) {
        this.type = type;
        this.navigation = navigation;
    }

    public String toString() {
        return String.format("%s %s", navigation.start().first(), type);
    }

    public String toDebugString() {
        return String.format("%s @%s", type.toDebugString(), navigation);
    }
}
