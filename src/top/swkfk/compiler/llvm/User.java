package top.swkfk.compiler.llvm;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.llvm.value.Value;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

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

    abstract public String toLLVM();
}
