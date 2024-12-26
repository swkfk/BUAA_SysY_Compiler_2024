package top.swkfk.compiler.arch.mips;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.helpers.ArrayInitialString;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mips 全局变量，生成在 .data 段
 */
final public class MipsGlobalVariable {
    private final SymbolType type;
    private final String tag;
    private final List<Integer> initializerList;
    private final String initializerString;

    MipsGlobalVariable(SymbolType type, String tag, List<Integer> initializerList, String initializerString) {
        this.type = type;
        this.tag = tag;
        this.initializerList = initializerList;
        this.initializerString = initializerString;
    }

    public MipsGlobalVariable(SymbolType type, String tag, List<Integer> initializerList) {
        this(type, tag, initializerList, null);
    }

    public MipsGlobalVariable(SymbolType type, String tag, String initializerString) {
        this(type, tag, null, initializerString);
    }

    /**
     * Is need to be aligned in .data segment
     *
     * @deprecated .word is aligned by Mars
     * @return Whether the item is 32-bit integer
     */
    public boolean needAlign() {
        return type.getFinalBaseType().is("i32");
    }

    @SuppressWarnings("SpellCheckingInspection")
    public String toMips() {
        StringBuilder sb = new StringBuilder();
        sb.append(tag).append(": ");
        // 下面，生成全局变量的初始化数据
        if (initializerList != null) {
            // 数组或普通变量初始化
            if (type.getFinalBaseType().is("i32")) {
                sb.append(".word ");
            } else {
                sb.append(".byte ");
            }
            // 生成初始化数据，每个数字之间用逗号隔开
            sb.append(initializerList.stream().map(String::valueOf).collect(Collectors.joining(", ")));
            // 字符串，弄个注释，方便查看
            if (type.getFinalBaseType().is("i8")) {
                sb.append("\t\t## '").append(ArrayInitialString.into(initializerList)).append("'");
            }
        } else if (initializerString != null) {
            // printf 中的字符串的初始化，注意，字符数组不在这里处理
            sb.append(".asciiz \"").append(initializerString.replace("\n", "\\n")).append("\"");
        } else {
            throw new RuntimeException("Both initializer List/String are null");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "MipsGlobalVariable {" +
                "type=" + type +
                ", tag='" + tag + '\'' +
                ", initializerList=" + initializerList +
                ", initializerString='" + initializerString + '\'' +
                '}';
    }
}
