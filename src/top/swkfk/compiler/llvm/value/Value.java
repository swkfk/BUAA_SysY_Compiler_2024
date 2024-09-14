package top.swkfk.compiler.llvm.value;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.llvm.Use;
import top.swkfk.compiler.helpers.GlobalCounter;

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
}
