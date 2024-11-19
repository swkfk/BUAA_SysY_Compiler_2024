package top.swkfk.compiler.llvm.value.instruction;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.frontend.symbol.type.TyPtr;
import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.Value;

final public class IAllocate extends User {

    @SuppressWarnings("SpellCheckingInspection")
    public IAllocate(SymbolType ty) {
        super("%" + Value.counter.get(), ty);
        assert ty instanceof TyPtr : "Only pointer type can be used in alloca";
    }

    @Override
    @SuppressWarnings("SpellCheckingInspection")
    public String toLLVM() {
        return getName() + " = alloca " + ((TyPtr) getType()).getBase().toString();
    }
}
