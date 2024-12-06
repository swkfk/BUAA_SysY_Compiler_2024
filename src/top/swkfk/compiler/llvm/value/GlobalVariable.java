package top.swkfk.compiler.llvm.value;

import top.swkfk.compiler.frontend.ast.declaration.object.ConstDef;
import top.swkfk.compiler.frontend.ast.declaration.object.VarDef;
import top.swkfk.compiler.frontend.symbol.SymbolVariable;
import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.frontend.symbol.type.TyArray;
import top.swkfk.compiler.helpers.ArrayInitialString;
import top.swkfk.compiler.llvm.value.constants.ConstInteger;
import top.swkfk.compiler.utils.Either;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final public class GlobalVariable extends Value {

    private final Either<Value, Map<Integer, Value>> initializer;

    /**
     * Global variable which is not an array. Initializer shall not be null even if it has no
     * initializer in the source file.
     *
     * @param name        variable name without '@' or mangling
     * @param type        variable's type
     * @param initializer variable's initial value
     */
    public GlobalVariable(String name, SymbolType type, Value initializer) {
        super(name, type);
        assert !type.is("array") : "Global variable cannot be an array without a list of initializers";
        this.initializer = Either.left(initializer);
    }

    /**
     * Global variable which is an array. Initializer shall be a map from index to value.
     *
     * @param name        variable name without '@' or mangling
     * @param type        variable's type
     * @param initializer variable's initial value
     */
    public GlobalVariable(String name, SymbolType type, Map<Integer, Value> initializer) {
        super(name, type);
        assert type.is("array") : "Global variable must be an array with a list of initializers";
        this.initializer = Either.right(initializer);
    }

    private static GlobalVariable from(SymbolVariable symbol) {
        if (symbol.getConstantValue().isLeft()) {
            return new GlobalVariable(
                symbol.getName(), symbol.getType(), symbol.getConstantValue().getLeft().into()
            );
        } else {
            return new GlobalVariable(
                symbol.getName(), symbol.getType(), symbol.getConstantValue().getRight().into()
            );
        }
    }

    public static GlobalVariable from(ConstDef def) {
        return from(def.getSymbol());
    }

    public static GlobalVariable from(VarDef def) {
        return from(def.getSymbol());
    }

    public List<Integer> getInitializerList() {
        return initializer.isLeft() ?
            List.of(((ConstInteger) initializer.getLeft()).getValue()) :
            mappedValueToListInteger(initializer.getRight());
    }

    private List<Integer> mappedValueToListInteger(Map<Integer, Value> map) {
        return IntStream.range(0, ((TyArray) getType()).getLength())
            .map(i -> ((ConstInteger) map.getOrDefault(i, new ConstInteger(0))).getValue())
            .boxed().collect(Collectors.toList());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("@").append(getName()).append(" = dso_local global ");
        if (initializer.isLeft()) {
            sb.append(initializer.getLeft());
        } else {
            sb.append(ArrayInitialString.into(getType(), initializer.getRight()));
        }
        return sb.toString();
    }
}
