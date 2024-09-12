package top.swkfk.compiler.llvm.value;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.utils.Either;

import java.util.Map;

final public class GlobalVariable extends Value {

    private final Either<Value, Map<Integer, Value>> initializer;

    /**
     * Global variable which is not an array. Initializer shall not be null even if it has no
     * initializer in the source file.
     * @param name variable name without '@' or mangling
     * @param type variable's type
     * @param initializer variable's initial value
     */
    public GlobalVariable(String name, SymbolType type, Value initializer) {
        super(name, type);
        assert !type.is("array") : "Global variable cannot be an array without a list of initializers";
        this.initializer = Either.left(initializer);
    }

    /**
     * Global variable which is an array. Initializer shall be a map from index to value.
     * @param name variable name without '@' or mangling
     * @param type variable's type
     * @param initializer variable's initial value
     */
    public GlobalVariable(String name, SymbolType type, Map<Integer, Value> initializer) {
        super(name, type);
        assert type.is("array") : "Global variable must be an array with a list of initializers";
        this.initializer = Either.right(initializer);
    }
}
