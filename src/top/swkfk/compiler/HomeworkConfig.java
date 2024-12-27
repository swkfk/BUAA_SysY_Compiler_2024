package top.swkfk.compiler;

/**
 * 作业配置，通过修改 {@link Hw} 来切换作业，并生成对应作业的默认输出文件名，方便测评。
 */
final public class HomeworkConfig {
    public enum Hw {
        Lexer, Syntax, Semantic, CodegenI, CodegenII
    }

    // 通过这里修改作业
    public static Hw hw = Hw.CodegenII;

    /**
     * 获取不同作业的默认输出目标文件名
     * @return 目标文件名
     */
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
