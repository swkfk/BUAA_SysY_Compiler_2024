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
    public IPhi(SymbolType type) {
        super("%" + Value.counter.get(), type);
    }

    public void addIncoming(BasicBlock block, Value value) {
        addOperand(block);
        addOperand(value);
    }

    public List<Pair<BasicBlock, Value>> getIncoming() {
        List<Pair<BasicBlock, Value>> incoming = new LinkedList<>();
        for (int i = 0; i < getOperands().size(); i += 2) {
            incoming.add(new Pair<>((BasicBlock) getOperand(i), getOperand(i + 1)));
        }
        return incoming;
    }

    public void dropIncomingIndex(int index) {
        dropOperand(index * 2);
        dropOperand(index * 2);
    }

    @Override
    public String toLLVM() {
        return getName() + " = phi " + getType() + " " +
            getIncoming().stream().map(
                pair -> "[ " + pair.second().getName() + ", %" + pair.first().getName() + " ]"
            ).collect(Collectors.joining(", "));
    }

    @Override
    public Integer numbering() {
        return getOperands().stream().map(Value::hashCode).reduce(0, Integer::sum);
    }

    @Override
    public boolean numberingEquals(User obj) {
        if (!(obj instanceof IPhi other)) {
            return false;
        }
        if (this.getOperands().size() != other.getOperands().size()) {
            return false;
        }
        for (int i = 0; i < this.getOperands().size(); i++) {
            if (this.getOperand(i).equals(other.getOperand(i))) {
                return false;
            }
        }
        return true;
    }
}
