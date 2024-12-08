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
    private String name;
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

    public void setName(String name) {
        this.name = name;
    }

    public SymbolType getType() {
        return type;
    }

    public void addUse(Use use) {
        uses.add(use);
    }

    public List<Use> getUses() {
        return uses;
    }

    public void removeSingleUse(Value user) {
        IntStream.range(0, uses.size())
            .filter(i -> uses.get(i).getValue().equals(user))
            .findFirst().ifPresent(uses::remove);
    }

    @SuppressWarnings("unused")
    public void removeAllUseOfUser(User user) {
        uses.removeIf(use -> use.getUser().equals(user));
    }

    public String toString() {
        return type + " " + name;
    }

    /**
     * Judge all values are constant integer. Normally used in constant folding or generating the mips
     * code to avoid instructions list <code>addiu $$, 1, 2</code>.
     * @param values values to be judged
     * @return whether all values are constant integer
     */
    @SuppressWarnings("SpellCheckingInspection")
    public static boolean allConstInteger(Value... values) {
        return Arrays.stream(values).allMatch(value -> value instanceof ConstInteger);
    }
}
