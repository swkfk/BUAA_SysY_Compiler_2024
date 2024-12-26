package top.swkfk.compiler.llvm;

import top.swkfk.compiler.Configure;
import top.swkfk.compiler.llvm.value.Function;
import top.swkfk.compiler.llvm.value.GlobalVariable;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final public class IrModule {
    private final List<Function> functions;
    private final Map<String, Function> externalFunctions;
    private final List<GlobalVariable> globalVariables;

    public IrModule(
        List<Function> functions,
        Map<String, Function> externalFunctions,
        List<GlobalVariable> globalVariables
    ) {
        this.functions = functions;
        this.externalFunctions = externalFunctions;
        this.globalVariables = globalVariables;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        // 输出代码头信息
        sb.append(";; Module: ").append(Configure.source).append("\n");

        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        sb.append(";; Compiled at: ").append(time).append("\n");

        // 输出外部函数的定义
        sb.append("\n;; External Functions:\n");
        for (Function externalFunction : externalFunctions.values()) {
            sb.append("declare ")
                .append(externalFunction.getType())
                .append(" @").append(externalFunction.getName()).append("(")
                .append(
                    externalFunction.getParams().stream()
                        .map(f -> f.getType().toString())
                        .collect(Collectors.joining(", "))
                ).append(")\n");
        }

        // 输出全局变量
        sb.append("\n;; Global Variables:\n");
        for (GlobalVariable globalVariable : globalVariables) {
            sb.append(globalVariable).append("\n");
        }

        // 输出函数
        sb.append("\n;; Functions:\n");
        for (Function function : functions) {
            sb.append(function).append("\n");
        }

        return sb.toString();
    }

    public List<Function> getFunctions() {
        return functions;
    }

    public List<GlobalVariable> getGlobalVariables() {
        return globalVariables;
    }

    /**
     * 跑一个 pass，根据调试配置输出中间结果
     * @param pass 指定的 pass 对象
     * @return Self
     */
    public IrModule runPass(Pass pass) {
        pass.run(this);
        if (Configure.debug.displayPassVerbose && pass.canPrintVerbose()) {
            // 针对需要输出的 pass，将中间结果输出到文件
            try (FileWriter writer = new FileWriter(
                Configure.passTarget
                    // Assume to be a '.' in the target file name
                    .replace("%(filename)", Configure.target.substring(0, Configure.target.lastIndexOf('.')))
                    .replace("%(pass-id)", String.format("%02d", pass.getPassID()))
                    .replace("%(pass-name)", pass.getName())
            )) {
                writer.write(";; After pass: " + pass.getName() + "\n");
                writer.write(this.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    @SuppressWarnings("unused")
    public IrModule runPass(Pass pass, boolean enable) {
        if (enable) {
            return runPass(pass);
        }
        return this;
    }
}
