package top.swkfk.compiler;

public class HomeworkConfig {
    public enum Hw {
        Lexer, Syntax, Semantic, Codegen
    }

    public static Hw hw = Hw.Syntax;
    //  Modify this  ^^  to change the homework  //

    public static String getTarget() {
        return switch (hw) {
            case Lexer -> "lexer.txt";
            case Syntax, Semantic -> "parser.txt";
            case Codegen -> "mips.txt";
        };
    }
}
