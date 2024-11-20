package top.swkfk.compiler;

public class HomeworkConfig {
    public enum Hw {
        Lexer, Syntax, Semantic, CodegenI, CodegenII
    }

    public static Hw hw = Hw.CodegenII;
    //  Modify this  ^^  to change the homework  //

    public static String getTarget() {
        return switch (hw) {
            case Lexer -> "lexer.txt";
            case Syntax -> "parser.txt";
            case Semantic -> "symbol.txt";
            case CodegenI -> "llvm_ir.txt";
            case CodegenII -> "mips.txt";
        };
    }
}
