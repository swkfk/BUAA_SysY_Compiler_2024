package top.swkfk.compiler.llvm.data_structure;

import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.llvm.value.Value;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Loop information record.
 * Direct reference: <a href="https://gitlab.eduxiji.net/educg-group-26173-2487151/T202410006203104-3288/-/blame/main/src/pass/utils/LoopInfo.java">compiler2024-x</a>
 * Whose author is the same as the author of this project. <br />
 * <img src="https://llvm.org/docs/_images/loop-terminology.svg" />
 */
final public class LoopInformation {
    private final BasicBlock header;
    private LoopInformation parent;

    private final List<LoopInformation> subLoops = new LinkedList<>();
    private final List<BasicBlock> blocks = new LinkedList<>();
    private final List<BasicBlock> exitBlocks = new LinkedList<>();
    private final List<BasicBlock> exitingBlocks = new LinkedList<>();
    private final List<BasicBlock> latchBlocks = new LinkedList<>();

    public LoopInformation(BasicBlock header) {
        blocks.add(header);
        this.header = header;
        this.parent = null;
    }

    public BasicBlock getHeader() {
        return header;
    }

    public List<BasicBlock> getBlocks() {
        return blocks;
    }

    public void addExitBlock(BasicBlock successor) {
        exitBlocks.add(successor);
    }

    public void addExitingBlock(BasicBlock block) {
        exitingBlocks.add(block);
    }

    public void addLatchBlock(BasicBlock predecessor) {
        latchBlocks.add(predecessor);
    }

    public boolean hasParent() {
        return parent != null;
    }

    public LoopInformation getParent() {
        return parent;
    }

    public void setParent(LoopInformation currentLoop) {
        this.parent = currentLoop;
    }

    public void addSubLoop(LoopInformation subLoop) {
        subLoops.add(subLoop);
    }

    public void addBlock(BasicBlock block) {
        blocks.add(block);
    }

    public void reverseBlocks() {
        BasicBlock first = blocks.remove(0);
        Collections.reverse(blocks);
        blocks.add(0, first);
    }

    public void reverseSubLoops() {
        Collections.reverse(subLoops);
    }

    public List<LoopInformation> getSubLoops() {
        return subLoops;
    }

    public List<BasicBlock> getLatch() {
        return latchBlocks;
    }

    public void insertBefore(BasicBlock block, BasicBlock newBlock) {
        int index = blocks.indexOf(block);
        blocks.add(index, newBlock);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("##").append(hashCode()).append("\n");
        sb.append("  Loop header: ").append(header.getName()).append("\n");
        sb.append("  Loop blocks: ").append(blocks.stream().map(BasicBlock::getName).collect(Collectors.joining(", "))).append("\n");
        sb.append("  Exit blocks: ").append(exitBlocks.stream().map(BasicBlock::getName).collect(Collectors.joining(", "))).append("\n");
        sb.append("  Exiting blocks: ").append(exitingBlocks.stream().map(BasicBlock::getName).collect(Collectors.joining(", "))).append("\n");
        sb.append("  Latch blocks: ").append(latchBlocks.stream().map(BasicBlock::getName).collect(Collectors.joining(", "))).append("\n");
        if (hasParent()) {
            sb.append("  Parent loop: ##").append(parent.hashCode()).append("\n");
        }

        return sb.toString();
    }
}
