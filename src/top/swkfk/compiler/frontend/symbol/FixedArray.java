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

final public class FixedArray {
    private final List<Integer> indices;
    private final HashMap<Integer, Integer> values;
    private final SymbolType baseType;

    public FixedArray(List<Integer> indices, SymbolType baseType) {
        this.indices = indices;
        this.values = new HashMap<>();
        this.baseType = baseType;
    }

    public void set(int index, ExprConst value) {
        values.put(index, value.calculate());
    }

    public int get(List<Expr> indices) {
        assert indices.size() == this.indices.size() : "Unmatched dimension";
        int index = indices.get(indices.size() - 1).calculateConst();
        int currentLength = 1;
        for (int i = indices.size() - 1; i > 0; i--) {
            currentLength *= this.indices.get(i);
            index += indices.get(i - 1).calculateConst() * currentLength;
        }
        return values.getOrDefault(index, 0);
    }

    public static FixedArray from(List<Integer> indices, ConstInitValue init, SymbolType baseType) {
        FixedArray array = new FixedArray(indices, baseType);
        if (init.getSubInitializers() != null) {
            /// Case 1: Normal array
            // According to the semantic rules, the number of elements in the initialization list must
            // be equal to the size of the array. So we can simply iterate through the list and set the
            // value of each element.
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

    public Map<Integer, Value> into() {
        Map<Integer, Value> result = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : values.entrySet()) {
            result.put(entry.getKey(), new ConstInteger(entry.getValue(), baseType));
        }
        return result;
    }
}
