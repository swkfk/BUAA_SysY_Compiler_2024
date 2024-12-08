package top.swkfk.compiler.llvm;

import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.Value;

final public class Use {
    private final Value value;
    private final User user;

    /**
     * The position of this use in the operand list of the User.
     */
    private final int position;

    public User getUser() {
        return user;
    }

    public Value getValue() {
        return value;
    }

    public Use(Value value, User user, int position) {
        this.value = value;
        this.user = user;
        this.position = position;
    }
}
