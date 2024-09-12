package top.swkfk.compiler.llvm;

import top.swkfk.compiler.llvm.value.Function;
import top.swkfk.compiler.llvm.value.GlobalVariable;

import java.util.List;

public class IrModule {
    private final List<Function> functions;
    private final List<GlobalVariable> globalVariables;

    public IrModule(List<Function> functions, List<GlobalVariable> globalVariables) {
        this.functions = functions;
        this.globalVariables = globalVariables;
    }
}
