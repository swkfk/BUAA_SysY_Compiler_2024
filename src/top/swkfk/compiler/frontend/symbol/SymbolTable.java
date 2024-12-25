package top.swkfk.compiler.frontend.symbol;

import top.swkfk.compiler.frontend.ast.declaration.function.FuncType;
import top.swkfk.compiler.frontend.symbol.type.SymbolType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

final public class SymbolTable {
    private static int counter = 1;  // Global scope is 1

    private final Map<String, SymbolVariable> allVariables;
    private final Map<String, SymbolFunction> allFunctions;
    private final Stack<Map<String, SymbolVariable>> stack;
    /// 栈中每个元素对应的符号表的 ID，不是层次，而是题目要求的编号
    private final Stack<Integer> stackOfScopeId;

    private final List<Symbol> outputList = new ArrayList<>();

    public SymbolTable() {
        allVariables = new HashMap<>();
        allFunctions = new HashMap<>();
        stack = new Stack<>() {{
            push(new HashMap<>()); // Global scope
        }};
        stackOfScopeId = new Stack<>() {{
            push(1); // Global scope
        }};
    }

    public void newScope() {
        counter++;
        stack.push(new HashMap<>());
        stackOfScopeId.push(counter);
    }

    public void exitScope() {
        stack.pop();
        stackOfScopeId.pop();
    }

    /**
     * 目的是在错误处理时，检查潜在的重定义，保留标识符不应该被重定义，虽然可能不是考察的内容
     * @param name 标识符名
     * @return 是否是保留标识符
     */
    @SuppressWarnings("SpellCheckingInspection")
    private boolean bumpKeepIdentifier(String name) {
        return name.equals("main") || name.equals("printf") || name.equals("getint") || name.equals("getchar");
    }

    /**
     * Add a variable to the current scope. If the variable already exists in the current scope or
     * is the same name with a function, return null.
     * @param name The name of the variable
     * @param type The type of the variable
     * @return The mangled name of the variable
     */
    public SymbolVariable addVariable(String name, SymbolType type) {
        if (stack.peek().containsKey(name) || bumpKeepIdentifier(name)) {
            // 这里表示重定义
            return null;
        }
        // stack.size() == 1 是判断是否是全局变量的标志
        SymbolVariable variable = new SymbolVariable(name, type, stack.size() == 1, stackOfScopeId.peek());
        allVariables.put(name, variable);
        stack.peek().put(name, variable);
        outputList.add(variable);
        return variable;
    }

    /**
     * Search for a variable in the current scope and all enclosing scopes.
     * @param name The name of the variable
     * @return The variable if found, null otherwise
     */
    public SymbolVariable getVariable(String name) {
        var iter = stack.listIterator(stack.size());
        while (iter.hasPrevious()) {
            // 从后往前找当前符号表栈中的变量
            var variables = iter.previous();
            if (variables.containsKey(name)) {
                SymbolVariable var = variables.get(name);
                // 符号的索引与名称当然要一致
                assert var.getName().equals(name) : "Inconsistent variable name for `" + name + "`";
                return var;
            }
        }
        // 找不到，返回 null
        return null;
    }

    public SymbolFunction addFunction(String name, FuncType type) {
        if (stack.peek().containsKey(name) || allFunctions.containsKey(name) || bumpKeepIdentifier(name)) {
            // 这里表示重定义
            return null;
        }
        SymbolFunction function = new SymbolFunction(name, SymbolType.from(type), stackOfScopeId.peek());
        allFunctions.put(name, function);
        outputList.add(function);
        return function;
    }

    public SymbolFunction getFunction(String name) {
        // 从函数符号表中查找函数，找不到会返回 null
        return allFunctions.get(name);
    }

    public SymbolVariable addParameter(SymbolFunction function, String name, SymbolType type) {
        SymbolVariable parameter = addVariable(name, type);
        if (parameter == null) {
            // 这里是 null，表示参数重定义了，返回 null 告知上层
            return null;
        }
        allVariables.put(name, parameter);
        stack.peek().put(name, parameter);
        function.addParameter(parameter);
        return parameter;
    }

    public String toDebugString() {
        return "==> Variables: \n" +
            allVariables.values().stream().map(Symbol::toDebugString).collect(Collectors.joining("\n"))
            + "\n==> Functions: \n" +
            allFunctions.values().stream().map(SymbolFunction::toDebugString).collect(Collectors.joining("\n"))
            ;
    }

    public String toString() {
        outputList.sort(Symbol::compareTo);
        return outputList.stream().map(Symbol::toString).collect(Collectors.joining("\n"));
    }
}
