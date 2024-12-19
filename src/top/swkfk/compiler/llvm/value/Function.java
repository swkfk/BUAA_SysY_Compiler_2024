package top.swkfk.compiler.llvm.value;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.llvm.data_structure.ControlFlowGraph;
import top.swkfk.compiler.llvm.data_structure.DominatorTree;
import top.swkfk.compiler.llvm.data_structure.LoopInformation;
import top.swkfk.compiler.utils.Container;
import top.swkfk.compiler.utils.DualLinkedList;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

final public class Function extends Value {

    private final DualLinkedList<BasicBlock> blocks;
    private final List<Value> params;
    private boolean external;

    public final Container<ControlFlowGraph> cfg = new Container<>();
    public final Container<DominatorTree> dom = new Container<>();
    public final Container<HashMap<BasicBlock, LoopInformation>> loopMap = new Container<>();
    public final Container<List<LoopInformation>> loops = new Container<>();
    public final Container<List<LoopInformation>> allLoops = new Container<>();

    private int counter;

    public int resumeCounter() {
        int counter = this.counter;
        this.counter = 0;
        return counter;
    }

    public void saveCounter(int counter) {
        this.counter = counter;
    }

    /**
     * Function is a value that represents a function.
     * @param name function name without '@' or mangling
     * @param type function's return type
     */
    public Function(String name, SymbolType type) {
        super(name, type);
        assert type.is("void") || type.is("i32") || type.is("i8") :
            "Function return type must be void or i32 or i8";
        this.blocks = new DualLinkedList<>();
        this.params = new LinkedList<>();
        this.external = false;
    }

    public static Function external(String name, SymbolType type, SymbolType... args) {
        Function function = new Function(name, type);
        for (SymbolType arg : args) {
            function.addParam(arg);
        }
        function.external = true;
        return function;
    }

    public boolean isExternal() {
        return external;
    }

    public void addBlock(BasicBlock block) {
        new DualLinkedList.Node<>(block).insertIntoTail(blocks);
    }

    public Value addParam(SymbolType type) {
        Value param = new Value(type);
        params.add(param);
        return param;
    }

    public List<Value> getParams() {
        return params;
    }

    public DualLinkedList<BasicBlock> getBlocks() {
        return blocks;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("define dso_local ").append(getType()).append(" @").append(getName()).append("(");
        sb.append(params.stream().map(Value::toString).collect(Collectors.joining(", ")));
        sb.append(") {\n");
        for (DualLinkedList.Node<BasicBlock> node : blocks) {
            sb.append(node.getData()).append("\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
}
