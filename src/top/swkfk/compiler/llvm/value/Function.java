package top.swkfk.compiler.llvm.value;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.utils.DualLinkedList;

final public class Function extends Value {

    private final DualLinkedList<Block> blocks;

    /**
     * Function is a value that represents a function.
     * @param name function name without '@' or mangling
     * @param type function's return type
     */
    public Function(String name, SymbolType type) {
        super(name, type);
        assert type.is("void") || type.is("i32") : "Function return type must be void or i32";
        this.blocks = new DualLinkedList<>();
    }

    public void addBlock(Block block) {
        new DualLinkedList.Node<>(block).insertIntoTail(blocks);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("define dso_local ").append(getType()).append(" @").append(getName()).append("(");
        // Skip Arguments
        sb.append(") {\n");
        for (DualLinkedList.Node<Block> node : blocks) {
            sb.append(node.getData()).append("\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
}
