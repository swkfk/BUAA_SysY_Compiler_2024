package top.swkfk.compiler;

/**
 * 运行参数配置，格式为：
 * <pre>
 *   java -jar Compiler.jar [-o &lt;output&gt;] [{-debug &lt;option&gt;}] [-error &lt;error-file&gt;]
 *   [-target mips] [-no-opt | -opt] [-dump &lt;dump-file&gt;] [-dump-vir &lt;dump-file&gt;] [source]
 * </pre>
 * 解释如下：
 * <ul>
 *     <li>{@code -o &lt;output&gt;}：指定输出文件，默认为每个作业的要求文件名。</li>
 *     <li>{@code -debug &lt;option&gt;}：开启调试选项，输出调试信息。可选项有：
 *     <ul>
 *         <li>{@code tokens}：显示词法分析结果。</li>
 *         <li>{@code errors}：显示错误信息。</li>
 *         <li>{@code symbols}：显示符号表。</li>
 *         <li>{@code pass-debug}：显示 Pass 的调试信息。</li>
 *         <li>{@code pass-verbose}：每个 Pass 后均输出中间代码。</li>
 *         <li>{@code opt-llvm}：额外输出优化后的中间代码。</li>
 *         <li>{@code .data}：输出 Mips 数据段信息。</li>
 *         <li>{@code vir}：输出使用虚拟寄存器的 MIPS 代码。</li>
 *     </ul>
 *     </li>
 *     <li>{@code -error &lt;error-file&gt;}：指定错误信息输出文件，默认为 {@code error.txt}。</li>
 *     <li>{@code -target mips}：指定后端指令集，目前只支持 'mips'</li>
 *     <li>{@code -no-opt | -opt}：是否开启优化，默认开启。</li>
 *     <li>{@code -dump &lt;dump-file&gt;}：指定输出优化后的中间代码的文件，默认为 {@code llvm_ir.txt}。</li>
 *     <li>{@code -dump-vir &lt;dump-file&gt;}：指定输出虚拟 MIPS 代码的文件，默认为 {@code virtual_mips.txt}。</li>
 *     <li>{@code source}：源代码文件，默认为 {@code testfile.txt}。</li>
 * </ul>
 */
@SuppressWarnings("SpellCheckingInspection")
final public class Configure {
    public enum Arch {
        mips
    }

    /// 源代码文件，从这里读取 SysY 代码
    public static String source = "testfile.txt";
    /// 默认的输出文件
    public static String target = HomeworkConfig.getTarget();
    /// 每一遍 Pass 的输出文件名（如果开启了相关选项则会输出）
    public static String passTarget = "%(filename)-%(pass-id)-%(pass-name).ll";
    /// 输出错误信息的默认文件名
    public static String error = "error.txt";
    /// 是否默认开启优化，请注意，RemovePhi 与寄存器分配是必然执行的
    public static boolean optimize = true;
    /// 输出优化后的 IR
    public static String dumpTarget = "llvm_ir.txt";
    /// 输出虚拟 MIPS 代码
    public static String dumpVirtualTarget = "virtual_mips.txt";
    /// 选用的目标架构
    public static Arch arch = Arch.mips;

    public static class debug {
        public static boolean displayTokens = false;
        public static boolean displayErrors = false;
        public static boolean displaySymbols = false;
        public static boolean displayPassDebug = false;
        public static boolean displayPassVerbose = false;
        public static boolean displayDataSegment = false;
        public static boolean dumpOptimizedIr = false;
        public static boolean dumpVirtualMips = false;

        /**
         * Display tokens with AST. For homework 3. Switch in {@link Controller#run()}.
         */
        public static boolean displayTokensWithAst = false;

        public static void parse(String arg) {
            switch (arg) {
                case "tokens" -> displayTokens = true;
                case "errors" -> displayErrors = true;
                case "symbols" -> displaySymbols = true;
                case "pass-debug" -> displayPassDebug = true;
                case "pass-verbose" -> displayPassVerbose = true;
                case "opt-llvm" -> dumpOptimizedIr = true;
                case ".data" -> displayDataSegment = true;
                case "vir" -> dumpVirtualMips = true;
                default -> throw new IllegalArgumentException("Unknown debug option: " + arg);
            }
        }
    }

    public static void parse(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-o")) {
                target = args[++i];
            } else if (args[i].equals("-debug")) {
                debug.parse(args[++i]);
            } else if (args[i].equals("-error")) {
                error = args[++i];
            } else if (args[i].equals("-target")) {
                arch = Arch.valueOf(args[++i]);
            } else if (args[i].equals("-no-opt")) {
                optimize = false;
            } else if (args[i].equals("-opt")) {
                optimize = true;
            } else if (args[i].equals("-dump")) {
                dumpTarget = args[++i];
            } else if (args[i].equals("-dump-vir")) {
                dumpVirtualTarget = args[++i];
            } else if (!args[i].startsWith("-")) {
                source = args[i];
            }
        }
    }
}
