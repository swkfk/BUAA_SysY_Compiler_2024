package top.swkfk.compiler;

final public class Configure {
    public enum Arch {
        mips
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static String source = "testfile.txt";
    public static String target = HomeworkConfig.getTarget();
    public static String passTarget = "%(filename)-%(pass-id)-%(pass-name).ll";
    public static String error = "error.txt";
    public static boolean optimize = false;
    public static Arch arch = Arch.mips;

    public static class debug {
        public static boolean displayTokens = false;
        public static boolean displayErrors = false;
        public static boolean displaySymbols = false;
        public static boolean displayPassDebug = false;
        public static boolean displayPassVerbose = false;
        public static boolean displayDataSegment = false;

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
                case ".data" -> displayDataSegment = true;
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
            } else if (!args[i].startsWith("-")) {
                source = args[i];
            }
        }
    }
}
