package top.swkfk.compiler.frontend.symbol;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class SymbolFunction extends Symbol {

    private final List<SymbolVariable> parameters;

    public SymbolFunction(String name, SymbolType type, int symbolTableIndex) {
        super(name, type, true, symbolTableIndex);
        parameters = new LinkedList<>();
    }

    public void addParameter(SymbolVariable parameter) {
        parameters.add(parameter);
    }

    public String toDebugString() {
        return super.toDebugString() + " (" +
            parameters.stream().map(Symbol::toDebugString).collect(Collectors.joining(", ")) +
            ")";
    }

    public List<SymbolVariable> getParameters() {
        return parameters;
    }
}
