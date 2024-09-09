package top.swkfk.compiler.frontend.symbol;

import java.util.LinkedList;
import java.util.List;

final public class SymbolFunction extends Symbol {

    private final List<SymbolVariable> parameters;

    public SymbolFunction(String name, Type type) {
        super(name, type, true);
        parameters = new LinkedList<>();
    }

    public void addParameter(SymbolVariable parameter) {
        parameters.add(parameter);
    }
}
