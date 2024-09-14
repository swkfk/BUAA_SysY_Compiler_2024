package top.swkfk.compiler.llvm.value;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.llvm.Use;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

abstract public class User extends Value {
    protected List<Value> operands;

    protected User(String name, SymbolType type) {
        super(name, type);
        this.operands = new LinkedList<>();
    }

    public void addOperand(Value operand) {
        operands.add(operand);
        Objects.requireNonNull(operand).addUse(new Use(operand, this, operands.size() - 1));
    }

    public Value getOperand(int index) {
        return operands.get(index);
    }

    public List<Value> getOperands() {
        return operands;
    }

    public void replaceOperand(int index, Value value) {
        Value old = operands.get(index);
        operands.set(index, value);
        if (old != null) {
            old.removeSingleUseOfUser(this);
        }
    }

    public void replaceOperand(Value old, Value value) {
        IntStream.range(0, operands.size())
            .filter(i -> operands.get(i).equals(old))
            .forEach(i -> replaceOperand(i, value));
    }

    abstract public String toLLVM();
}
