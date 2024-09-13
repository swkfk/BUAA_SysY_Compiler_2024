package top.swkfk.compiler.frontend.symbol.type;

import top.swkfk.compiler.frontend.ast.expression.ExprConst;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

final public class TyArray extends SymbolType {

    private final SymbolType base;
    private final int length;

    public TyArray(SymbolType base, int length) {
        this.base = base;
        this.length = length;
    }

    @Override
    public boolean is(String type) {
        return type.equalsIgnoreCase(toString()) || type.equalsIgnoreCase("array");
    }

    @Override
    public int sizeof() {
        return base.sizeof() * length;
    }

    public int getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "[" + length + " x " + base + "]";
    }

    /**
     * Create an array or a basic i32 type from a list of indices.
     * @param indices The list of indices, empty means a basic i32 type.
     * @return The created type.
     */
    public static SymbolType from(List<ExprConst> indices) {
        SymbolType base = Ty.I32;
        for (int i = indices.size() - 1; i >= 0; i--) {
            if (indices.get(i) == null) {
                base = new TyPtr(base);
            } else {
                base = new TyArray(base, indices.get(i).calculate());
            }
        }
        return base;
    }

    public List<Integer> getIndices() {
        if (base instanceof TyArray baseArray) {
            return Collections.unmodifiableList(new LinkedList<>() {{
                add(length);
                addAll(baseArray.getIndices());
            }});
        } else {
            return Collections.singletonList(length);
        }
    }

    public SymbolType getBase() {
        return base;
    }
}
