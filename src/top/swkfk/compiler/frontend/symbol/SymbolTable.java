package top.swkfk.compiler.frontend.symbol;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

final public class SymbolTable {
    private final Map<String, SymbolVariable> allVariables;
    private final Map<String, SymbolFunction> allFunctions;
    private final Stack<Map<String, SymbolVariable>> stack;

    public SymbolTable() {
        allVariables = new HashMap<>();
        allFunctions = new HashMap<>();
        stack = new Stack<>() {{
            push(new HashMap<>()); // Global scope
        }};
    }

    public void newScope() {
        stack.push(new HashMap<>());
    }

    public void exitScope() {
        stack.pop();
    }

    /**
     * Add a variable to the current scope. If the variable already exists in the current scope or
     * is the same name with a function, return null.
     * @param name The name of the variable
     * @param type The type of the variable
     * @return The mangled name of the variable
     */
    @SuppressWarnings("SpellCheckingInspection")
    public String addVariable(String name, Symbol.Type type) {
        if (stack.peek().containsKey(name) || allFunctions.containsKey(name)
            || name.equals("main") || name.equals("printf") || name.equals("getint")
        ) {
            return null;
        }
        SymbolVariable variable = new SymbolVariable(name, type, stack.size() == 1);
        allVariables.put(name, variable);
        stack.peek().put(name, variable);
        return variable.getMangle();
    }

    /**
     * Search for a variable in the current scope and all enclosing scopes.
     * @param name The name of the variable
     * @return The variable if found, null otherwise
     */
    public SymbolVariable getVariable(String name) {
        for (Map<String, SymbolVariable> variables : stack) {
            if (variables.containsKey(name)) {
                SymbolVariable var = variables.get(name);
                assert var.getName().equals(name) : "Inconsistent variable name for `" + name + "`";
                return var;
            }
        }
        return null;
    }

    public SymbolFunction addFunction(String name, Symbol.Type type) {
        SymbolFunction function = new SymbolFunction(name, type);
        allFunctions.put(name, function);
        return function;
    }

    public SymbolVariable addParameter(SymbolFunction function, String name, Symbol.Type type) {
        SymbolVariable parameter = new SymbolVariable(name, type, false);
        allVariables.put(name, parameter);
        stack.peek().put(name, parameter);
        function.addParameter(parameter);
        return parameter;
    }
}
