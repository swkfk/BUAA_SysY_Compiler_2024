package top.swkfk.compiler;

public class Configure {
    public static String source = "testfile.txt";
    public static String target = "output.txt";

    public static void parse(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-o")) {
                target = args[++i];
            } else {
                source = args[i];
            }
        }
    }
}
