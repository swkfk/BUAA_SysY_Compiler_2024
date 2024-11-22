package top.swkfk.compiler.llvm.value;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.llvm.Use;
import top.swkfk.compiler.helpers.GlobalCounter;
import top.swkfk.compiler.llvm.value.constants.ConstInteger;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

public class Value {
    private final String name;
    private final SymbolType type;
    private final List<Use> uses;

    public final static GlobalCounter counter = new GlobalCounter();

    public Value(SymbolType type) {
        this("%" + counter.get(), type);
    }

    public Value(String name, SymbolType type) {
        this.name = name;
        this.type = type;
        this.uses = new LinkedList<>();
    }

    public String getName() {
        return name;
    }

    public SymbolType getType() {
        return type;
    }

    public void addUse(Use use) {
        uses.add(use);
    }

    public void removeSingleUseOfUser(User user) {
        IntStream.range(0, uses.size())
            .filter(i -> uses.get(i).getUser().equals(user))
            .findFirst().ifPresent(uses::remove);
    }

    public void removeAllUseOfUser(User user) {
        uses.removeIf(use -> use.getUser().equals(user));
    }

    public String toString() {
        return type + " " + name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Value) {
            return name.equals(((Value) obj).name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Judge all values are constant integer. Normally used in constant folding or generating the mips
     * code to avoid instructions list addiu $$, 1, 2.
     * @param values values to be judged
     * @return whether all values are constant integer
     */
    public static boolean allConstInteger(Value... values) {
        return Arrays.stream(values).allMatch(value -> value instanceof ConstInteger);
    }
}
