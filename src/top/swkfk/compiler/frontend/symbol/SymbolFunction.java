package top.swkfk.compiler.frontend.symbol;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

final public class SymbolFunction extends Symbol {

    private final List<SymbolVariable> parameters;

    public SymbolFunction(String name, SymbolType type) {
        super(name, type, true);
        parameters = new LinkedList<>();
    }

    public void addParameter(SymbolVariable parameter) {
        parameters.add(parameter);
    }

    public String toString() {
        return super.toString() + " (" +
            parameters.stream().map(Symbol::toString).collect(Collectors.joining(", ")) +
            ")";
    }
}
