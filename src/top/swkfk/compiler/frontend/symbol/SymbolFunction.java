package top.swkfk.compiler.frontend.symbol;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 函数符号，主要包含了函数的参数列表
 */
final public class SymbolFunction extends Symbol {

    /// 函数的参数列表，均为变量符号，且有属性 {@link SymbolVariable#isFromParam()} 为 true
    private final List<SymbolVariable> parameters;

    public SymbolFunction(String name, SymbolType type, int symbolTableIndex) {
        // 函数符号是全局的
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

    @Override
    public String toString() {
        return super.toString() + " " + SymbolType.getDisplayString(getType()) + "Func";
    }

    public List<SymbolVariable> getParameters() {
        return parameters;
    }
}
