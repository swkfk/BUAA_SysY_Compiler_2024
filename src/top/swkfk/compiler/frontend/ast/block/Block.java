package top.swkfk.compiler.frontend.ast.block;

import top.swkfk.compiler.frontend.ast.ASTNode;

import java.util.LinkedList;
import java.util.List;

final public class Block extends ASTNode {
    private final List<BlockItem> blocks;

    public Block() {
        this.blocks = new LinkedList<>();
    }

    public void addBlock(BlockItem block) {
        blocks.add(block);
    }

    public List<BlockItem> getItems() {
        return blocks;
    }

    @Override
    protected String getName() {
        return "<Block>";
    }
}
