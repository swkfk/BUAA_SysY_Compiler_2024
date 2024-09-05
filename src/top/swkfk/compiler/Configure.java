package top.swkfk.compiler;

public class Configure {
    @SuppressWarnings("SpellCheckingInspection")
    public static String source = "testfile.txt";
    public static String target = "output.txt";

    public static class debug {
        public static boolean displayTokens = false;

        /**
         * Display tokens with AST. For homework 3. Switch in {@link Controller#frontend()}.
         */
        public static boolean displayTokensWithAst = false;

        @SuppressWarnings("SwitchStatementWithTooFewBranches")
        public static void parse(String arg) {
            switch (arg) {
                case "tokens" -> displayTokens = true;
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
            } else if (!args[i].startsWith("-")) {
                source = args[i];
            }
        }
    }
}
