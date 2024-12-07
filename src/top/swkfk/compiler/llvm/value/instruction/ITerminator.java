package top.swkfk.compiler.llvm.value.instruction;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.BasicBlock;

abstract public class ITerminator extends User {

    protected ITerminator(String name, SymbolType type) {
        super(name, type);
    }

    @SuppressWarnings("unused")
    abstract public BasicBlock[] getSuccessors();
}
