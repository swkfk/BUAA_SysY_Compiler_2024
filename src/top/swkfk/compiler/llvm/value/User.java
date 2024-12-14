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
        this.addUse(new Use(operand, this, operands.size() - 1));
    }

    public void addOperandNullable(Value operand) {
        operands.add(operand);
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
            this.removeSingleUse(old);
        }
        this.addUse(new Use(value, this, index));
    }

    public void replaceOperand(Value old, Value value) {
        IntStream.range(0, operands.size())
            .filter(i -> operands.get(i).equals(old))
            .forEach(i -> replaceOperand(i, value));
    }

    public void dropOperand(int index) {
        Value old = operands.get(index);
        operands.remove(index);
        this.removeSingleUse(old);
    }

    public Integer numbering() {
        return null;
    }

    public boolean numberingEquals(User other) {
        return false;
    }

    abstract public String toLLVM();
}
