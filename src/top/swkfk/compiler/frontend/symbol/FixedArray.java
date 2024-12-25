package top.swkfk.compiler.frontend.symbol;

import top.swkfk.compiler.frontend.ast.declaration.object.ConstInitValue;
import top.swkfk.compiler.frontend.ast.expression.Expr;
import top.swkfk.compiler.frontend.ast.expression.ExprAdd;
import top.swkfk.compiler.frontend.ast.expression.ExprConst;
import top.swkfk.compiler.frontend.ast.expression.ExprMul;
import top.swkfk.compiler.frontend.ast.expression.ExprPrimary;
import top.swkfk.compiler.frontend.ast.expression.ExprUnaryPrimary;
import top.swkfk.compiler.frontend.ast.misc.Char;
import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.llvm.value.constants.ConstInteger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * '固定数组'，用于表示常量表达式的数组
 */
final public class FixedArray {
    /// 数组索引，理论上这里支持多维数组，但是由于今年不需要支持多维数组，所以这里只有一个维度
    private final List<Integer> indices;
    /// 值的映射，key 为索引，value 为值
    private final HashMap<Integer, Integer> values;
    /// 数组的基本类型
    private final SymbolType baseType;

    public FixedArray(List<Integer> indices, SymbolType baseType) {
        this.indices = indices;
        this.values = new HashMap<>();
        this.baseType = baseType;
    }

    public void set(int index, ExprConst value) {
        values.put(index, value.calculate());
    }

    /**
     * 根据给定的索引获取数组中的值
     * @param indices 索引列表，这里需要是编译期可计算的常量表达式
     * @return 数组中的值
     */
    public int get(List<Expr> indices) {
        assert indices.size() == this.indices.size() : "Unmatched dimension";
        int index = indices.get(indices.size() - 1).calculateConst();
        int currentLength = 1;
        for (int i = indices.size() - 1; i > 0; i--) {
            currentLength *= this.indices.get(i);
            index += indices.get(i - 1).calculateConst() * currentLength;
        }
        // 注意，这里认为有全部的默认值为 0
        return values.getOrDefault(index, 0);
    }

    /**
     * 将一个初始化列表转换为固定数组，简化为只支持一维数组！！！
     * @param indices 索引列表
     * @param init 初始化列表
     * @param baseType 数组的基本类型
     * @return 固定数组
     */
    public static FixedArray from(List<Integer> indices, ConstInitValue init, SymbolType baseType) {
        FixedArray array = new FixedArray(indices, baseType);
        if (init.getSubInitializers() != null) {
            /// Case 1: Normal array
            // According to the semantic rules, the number of elements in the initialization list must
            // be equal to the size of the array. So we can simply iterate through the list and set the
            // value of each element.
            // 事实上，这里也支持更小的初始化列表，不会出问题
            int index = 0;
            Queue<ConstInitValue> queue = new LinkedList<>();
            queue.add(init);
            while (!queue.isEmpty()) {
                ConstInitValue current = queue.poll();
                if (current.getExpr() != null) {
                    array.set(index++, current.getExpr());
                } else {
                    queue.addAll(current.getSubInitializers());
                }
            }
        } else {
            /// Case 2: String constant
            // In this case, only set the array with the string constant.
            // 这里搞这么复杂，是为了将它们转成常量表达式，因为 FixedArray#set 方法只接受常量表达式，而我
            // 懒得写一个新的方法
            String stringConst = init.getStringConst();
            for (int i = 0; i < stringConst.length(); i++) {
                array.set(i, new ExprConst(new ExprAdd(new ExprMul(new ExprUnaryPrimary(new ExprPrimary(
                    new Char(stringConst.charAt(i))
                ))))));
            }
            array.set(stringConst.length(), new ExprConst(new ExprAdd(new ExprMul(new ExprUnaryPrimary(
                new ExprPrimary(new Char('\0'))
            )))));
        }
        return array;
    }

    @Override
    public String toString() {
        return values.toString();
    }

    /**
     * 将固定数组转换为 LLVM IR 中的数组初始化列表
     * @return LLVM IR 中的数组初始化列表（映射）
     */
    public Map<Integer, Value> into() {
        Map<Integer, Value> result = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : values.entrySet()) {
            result.put(entry.getKey(), new ConstInteger(entry.getValue(), baseType));
        }
        return result;
    }
}
