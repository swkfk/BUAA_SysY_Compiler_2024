package top.swkfk.compiler.llvm.value;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.llvm.Use;
import top.swkfk.compiler.helpers.GlobalCounter;

import java.util.LinkedList;
import java.util.List;

public class Value {
    private final String name;
    private final SymbolType type;
    private final List<Use> uses;

    public final static GlobalCounter counter = new GlobalCounter();

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
}
