package top.swkfk.compiler;

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
