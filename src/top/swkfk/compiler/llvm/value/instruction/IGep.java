package top.swkfk.compiler.llvm.value.instruction;

import top.swkfk.compiler.frontend.symbol.type.TyArray;
import top.swkfk.compiler.frontend.symbol.type.TyPtr;
import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.Value;

/**
 * Get element pointer. We only allow a single level dereference once, in order to simplify the
 * optimization process.
 */
final public class IGep extends User {

    /**
     * Whether the offset is loaded from an argument of a function. In order to make the Mem2Reg
     * optimization work, we need to put the first argument of 'i32 0' outside the gep as a 'load'.
     */
    private final boolean loadedFromArgument;

    public IGep(Value pointer, Value offset, boolean loadedFromArgument) {
        super("%" + Value.counter.get(),
            loadedFromArgument ?
                // ( Arg: int [][2] ==> (int [2])* ) -> ( Loaded: (int [2])* ) -> (int [2])*, invoke
                // this once even if the dimension is more than one.
                new TyPtr(((TyPtr) pointer.getType()).getBase()) :
                // (int [2][3])* -> (int [2])* -> int *, global or local array is allocated as a pointer
                new TyPtr(((TyArray) ((TyPtr) pointer.getType()).getBase()).getBase())
        );
        assert pointer.getType().is("ptr") : "Only pointer type can be calculated";
        assert offset.getType().is("int") : "Only integer type can be used as offset";
        this.loadedFromArgument = loadedFromArgument;
        addOperand(pointer);
        addOperand(offset);
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Override
    public String toLLVM() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append(" = getelementptr ");
        sb.append(((TyPtr) getOperand(0).getType()).getBase()).append(", ");
        sb.append(getOperand(0)).append(", ");
        if (!loadedFromArgument) {
            sb.append("i32 0, ");
        }
        sb.append(getOperand(1));
        return sb.toString();
    }
}
