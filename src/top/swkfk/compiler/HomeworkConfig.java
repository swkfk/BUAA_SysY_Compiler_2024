package top.swkfk.compiler;

public class HomeworkConfig {
    public enum Hw {
        Lexer, Syntax, Semantic, Codegen
    }

    public static Hw hw = Hw.Semantic;
    //  Modify this  ^^  to change the homework  //

    public static String getTarget() {
        return switch (hw) {
            case Lexer -> "lexer.txt";
            case Syntax -> "parser.txt";
            case Semantic -> "symbol.txt";
            case Codegen -> "mips.txt";
        };
    }
}
