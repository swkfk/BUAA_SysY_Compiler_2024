package top.swkfk.compiler.arch.mips;

import top.swkfk.compiler.Configure;
import top.swkfk.compiler.arch.ArchModule;
import top.swkfk.compiler.arch.mips.process.MipsGenerator;
import top.swkfk.compiler.llvm.IrModule;
import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.llvm.value.Function;
import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.utils.DualLinkedList;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class MipsModule implements ArchModule {
    private final List<MipsFunction> functions = new LinkedList<>();

    @Override
    public ArchModule runParseIr(IrModule module) {
        //

        module.getFunctions().forEach(this::parseFunction);

        return this;
    }

    private void parseFunction(Function function) {
        MipsGenerator generator = new MipsGenerator(function.getBlocks());
        MipsFunction mipsFunction = new MipsFunction(function.getName());
        functions.add(mipsFunction);
        // TODO: calculate the parameter size
        MipsBlock entry = new MipsBlock();
        mipsFunction.addBlock(entry);
        for (DualLinkedList.Node<BasicBlock> node : function.getBlocks()) {
            parseBlock(node.getData(), mipsFunction, generator);
        }
        // TODO: fill the entry block
    }

    private void parseBlock(BasicBlock block, MipsFunction mipsFunction, MipsGenerator generator) {
        MipsBlock mipsBlock = generator.blockLLVM2Mips(block);
        mipsFunction.addBlock(mipsBlock);
        block.getInstructions().forEach(node -> parseInstruction(node.getData(), mipsBlock, generator));
    }

    private void parseInstruction(User instruction, MipsBlock mipsBlock, MipsGenerator generator) {
        mipsBlock.reservedComment = instruction.toLLVM();
        generator.run(instruction).forEach(mipsBlock::addInstruction);
        mipsBlock.reservedComment = null;  // Maybe there are no instruction added
    }

    @Override
    public ArchModule runRemovePhi() {
        return this;
    }

    @Override
    public ArchModule runVirtualOptimize() {
        return this;
    }

    @Override
    public ArchModule runRegisterAllocation() {
        return this;
    }

    @Override
    public ArchModule runPhysicalOptimize() {
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Module: ").append(Configure.source).append("\n");

        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        sb.append("# Compiled at: ").append(time).append("\n");

        sb.append("\n.data\n");

        //

        sb.append("\n.text\n\n");

        for (MipsFunction function : functions) {
            sb.append(function.toMips()).append("\n");
        }

        return sb.toString();
    }
}
