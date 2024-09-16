package top.swkfk.compiler.llvm.value.instruction;

import top.swkfk.compiler.frontend.symbol.SymbolFunction;
import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.Value;

import java.util.List;
import java.util.stream.Collectors;

final public class ICall extends User {
    private final SymbolFunction function;

    public ICall(SymbolFunction function, List<Value> arguments) {
        // If the function return 'void', we will ignore its value name.
        super("%" + Value.counter.get(), function.getType());
        this.function = function;
        for (Value arg : arguments) {
            addOperand(arg);
        }
    }

    @Override
    public String toLLVM() {
        StringBuilder sb = new StringBuilder();
        if (function.getType().is("void")) {
            sb.append("call void ");
        } else {
            sb.append(getName()).append(" = call ").append(function.getType()).append(" ");
        }
        sb.append("@").append(function.getName()).append("(");
        sb.append(
            getOperands().stream().map(Value::toString).collect(Collectors.joining(", "))
        );
        sb.append(")");
        return sb.toString();
    }
}
