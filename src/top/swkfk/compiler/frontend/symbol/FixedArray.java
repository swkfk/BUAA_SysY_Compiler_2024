package top.swkfk.compiler.frontend.symbol;

import top.swkfk.compiler.frontend.ast.declaration.object.ConstInitValue;
import top.swkfk.compiler.frontend.ast.expression.Expr;
import top.swkfk.compiler.frontend.ast.expression.ExprConst;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

final public class FixedArray {
    private final List<Integer> indices;
    private final HashMap<Integer, Integer> values;

    public FixedArray(List<Integer> indices) {
        this.indices = indices;
        this.values = new HashMap<>();
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

    public static FixedArray from(List<Integer> indices, ConstInitValue init) {
        FixedArray array = new FixedArray(indices);
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
        return array;
    }
}
