package top.swkfk.compiler.arch.mips;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.helpers.ArrayInitialString;

import java.util.List;
import java.util.stream.Collectors;

public class MipsGlobalVariable {
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
        if (initializerList != null) {
            if (type.getFinalBaseType().is("i32")) {
                sb.append(".word ");
            } else {
                sb.append(".byte ");
            }
            sb.append(initializerList.stream().map(String::valueOf).collect(Collectors.joining(", ")));
            if (type.getFinalBaseType().is("i8")) {
                sb.append("\t\t## '").append(ArrayInitialString.into(initializerList)).append("'");
            }
        } else if (initializerString != null) {
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
