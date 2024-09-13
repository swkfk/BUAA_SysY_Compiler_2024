package top.swkfk.compiler.llvm.value.instruction;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.utils.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

final public class IPhi extends User {
    private final List<Pair<BasicBlock, Value>> incoming;

    public IPhi(SymbolType type) {
        super("%" + Value.counter.get(), type);
        incoming = new LinkedList<>();
    }

    public void addIncoming(BasicBlock block, Value value) {
        incoming.add(new Pair<>(block, value));
        addOperand(value);
    }

    @Override
    public String toLLVM() {
        return getName() + " = phi " + getType() +
            " [" + incoming.stream().map(
                pair -> pair.second().getName() + ", %" + pair.first().getName()
            ).collect(Collectors.joining(", ")) + "]";
    }
}
