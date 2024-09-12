package top.swkfk.compiler.llvm;

import top.swkfk.compiler.llvm.type.Type;
import top.swkfk.compiler.llvm.value.Value;

import java.util.LinkedList;
import java.util.List;

abstract public class User extends Value {
    protected List<Value> operands;

    protected User(String name, Type type) {
        super(name, type);
        this.operands = new LinkedList<>();
    }

    abstract public String toLLVM();
}
