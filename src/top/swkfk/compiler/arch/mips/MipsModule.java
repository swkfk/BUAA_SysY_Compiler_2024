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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MipsModule implements ArchModule {
    private final List<MipsFunction> functions = new LinkedList<>();
    private final Map<Function, MipsFunction> functionMap = new HashMap<>();

    @Override
    public ArchModule runParseIr(IrModule module) {
        //

        module.getFunctions().forEach(this::parseFunction);

        return this;
    }

    private void parseFunction(Function function) {
        MipsBlock entry = new MipsBlock();
        MipsBlock exit = new MipsBlock();

        MipsGenerator generator = new MipsGenerator(function.getBlocks(), functionMap, exit);
        MipsFunction mipsFunction = new MipsFunction(function.getName());
        functions.add(mipsFunction);
        functionMap.put(function, mipsFunction);
        // TODO: calculate the parameter size

        mipsFunction.addBlock(entry);
        for (DualLinkedList.Node<BasicBlock> node : function.getBlocks()) {
            parseBlock(node.getData(), mipsFunction, generator);
        }
        // TODO: fill the entry block

        mipsFunction.addBlock(exit);

        // TODO: fill the exit block
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
            sb.append("# Function: ").append(function).append("\n");
            sb.append(function.toMips()).append("\n");
        }

        return sb.toString();
    }
}
