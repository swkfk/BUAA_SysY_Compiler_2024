package top.swkfk.compiler.llvm;

import top.swkfk.compiler.Configure;
import top.swkfk.compiler.llvm.value.Function;
import top.swkfk.compiler.llvm.value.GlobalVariable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class IrModule {
    private final List<Function> functions;
    private final List<GlobalVariable> globalVariables;

    public IrModule(List<Function> functions, List<GlobalVariable> globalVariables) {
        this.functions = functions;
        this.globalVariables = globalVariables;
    }

    @SuppressWarnings("SpellCheckingInspection")
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(";; Module: ").append(Configure.source).append("\n");

        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        sb.append(";; Compiled at: ").append(time).append("\n");

        sb.append("\n;; External Functions:\n");
        sb.append("declare i32 @getint()\n");
        sb.append("declare void @putint()\n");
        sb.append("declare void @putch()\n");
        sb.append("declare void @putstr(i8*)\n");

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
