package top.swkfk.compiler.helpers;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.frontend.symbol.type.TyArray;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.llvm.value.constants.ConstInteger;
import top.swkfk.compiler.utils.Pair;

import java.util.List;
import java.util.Map;

/**
 * 生成适用于 LLVM 的数组初始化字符串
 */
final public class ArrayInitialString {
    /**
     * Generate the string representation of an array initializer. Return the string and whether the
     * initializer is all zero.
     * @param ty current array type or the final base type
     * @param init initial values
     * @param start current starting index
     * @return pair of string and whether the initializer is all zero
     */
    @SuppressWarnings("SpellCheckingInspection")
    private static Pair<String, Boolean> work(SymbolType ty, Map<Integer, Value> init, int start) {
        StringBuilder sb = new StringBuilder();
        if (!ty.is("array")) {
            if (init.getOrDefault(start, ConstInteger.zero).equals(ConstInteger.zero)) {
                return new Pair<>(ty.getFinalBaseType() + " 0", true);
            } else {
                return new Pair<>(init.get(start).toString(), false);
            }
        }
        TyArray tyArray = (TyArray) ty;
        SymbolType tyBase = tyArray.getBase();
        sb.append(tyArray).append(" ");

        StringBuilder innerBuilder = new StringBuilder();
        boolean allZero = true;

        innerBuilder.append("[");
        for (int i = 0; i < tyArray.getLength(); i++) {
            Pair<String, Boolean> res = work(tyBase, init, start + i * tyBase.sizeof() / ty.getFinalBaseType().sizeof());
            innerBuilder.append(res.first());
            if (i != tyArray.getLength() - 1) {
                innerBuilder.append(", ");
            }
            allZero &= res.second();
        }
        innerBuilder.append("]");

        if (allZero) {
            sb.append("zeroinitializer");
        } else {
            sb.append(innerBuilder);
        }

        return new Pair<>(sb.toString(), allZero);
    }

    public static String into(SymbolType ty, Map<Integer, Value> init) {
        return work(ty, init, 0).first();
    }

    public static String into(List<Integer> list) {
        StringBuilder sb = new StringBuilder();
        for (Integer integer : list) {
            if (Character.isLetterOrDigit(integer)) {
                sb.append((char) integer.intValue());
            } else if (integer == '\n') {
                sb.append("\\n");
            } else if (integer == '\t') {
                sb.append("\\t");
            } else {
                sb.append("\\x").append(Integer.toHexString(integer));
            }
        }
        return sb.toString();
    }
}
