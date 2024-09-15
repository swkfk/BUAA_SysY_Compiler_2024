package top.swkfk.compiler.llvm;

import top.swkfk.compiler.Configure;
import top.swkfk.compiler.frontend.symbol.SymbolFunction;
import top.swkfk.compiler.llvm.value.Function;
import top.swkfk.compiler.llvm.value.GlobalVariable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IrModule {
    private final List<Function> functions;
    private final Map<String, SymbolFunction> externalFunctions;
    private final List<GlobalVariable> globalVariables;

    public IrModule(
        List<Function> functions,
        Map<String, SymbolFunction> externalFunctions,
        List<GlobalVariable> globalVariables
    ) {
        this.functions = functions;
        this.externalFunctions = externalFunctions;
        this.globalVariables = globalVariables;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(";; Module: ").append(Configure.source).append("\n");

        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        sb.append(";; Compiled at: ").append(time).append("\n");

        sb.append("\n;; External Functions:\n");
        for (SymbolFunction externalFunction : externalFunctions.values()) {
            sb.append("declare ")
                .append(externalFunction.getType())
                .append(" @").append(externalFunction.getName()).append("(")
                .append(
                    externalFunction.getParameters().stream()
                        .map(f -> f.getType().toString())
                        .collect(Collectors.joining(", "))
                ).append(")\n");
        }

        sb.append("\n;; Global Variables:\n");
        for (GlobalVariable globalVariable : globalVariables) {
            sb.append(globalVariable).append("\n");
        }

        sb.append("\n;; Functions:\n");
        for (Function function : functions) {
            sb.append(function).append("\n");
        }

        return sb.toString();
    }
}
