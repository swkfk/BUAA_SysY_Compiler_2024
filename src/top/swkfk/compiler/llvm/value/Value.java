package top.swkfk.compiler.llvm.value;

import top.swkfk.compiler.llvm.Use;
import top.swkfk.compiler.llvm.type.Type;

import java.util.LinkedList;
import java.util.List;

public class Value {
    private final String name;
    private final Type type;
    private final List<Use> uses;

    public Value(String name, Type type) {
        this.name = name;
        this.type = type;
        this.uses = new LinkedList<>();
    }
}
