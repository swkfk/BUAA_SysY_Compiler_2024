package top.swkfk.compiler.frontend.symbol;

import top.swkfk.compiler.frontend.ast.expression.ExprConst;

import java.util.HashMap;
import java.util.List;

final public class FixedArray {
    private final List<Integer> indices;
    private final HashMap<Integer, Integer> values;

    public FixedArray(List<ExprConst> indices) {
        this.indices = indices.stream().map(ExprConst::calculate).toList();
        this.values = new HashMap<>();
    }

    public void set(ExprConst index, ExprConst value) {
        values.put(index.calculate(), value.calculate());
    }

    public int get(List<ExprConst> indices) {
        assert indices.size() == this.indices.size() : "Unmatched dimension";
        int index = indices.get(indices.size() - 1).calculate();
        int currentLength = 1;
        for (int i = indices.size() - 1; i > 0; i--) {
            currentLength *= this.indices.get(i);
            index += indices.get(i - 1).calculate() * currentLength;
        }
        return values.get(index);
    }
}
