package top.swkfk.compiler.arch.mips;

import top.swkfk.compiler.Configure;
import top.swkfk.compiler.arch.ArchModule;
import top.swkfk.compiler.arch.mips.instruction.MipsIBinary;
import top.swkfk.compiler.arch.mips.instruction.MipsIJump;
import top.swkfk.compiler.arch.mips.instruction.MipsILoadStore;
import top.swkfk.compiler.arch.mips.operand.MipsImmediate;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.process.MipsFunctionRegisterAllocate;
import top.swkfk.compiler.arch.mips.process.MipsFunctionRemovePhi;
import top.swkfk.compiler.arch.mips.process.MipsGenerator;
import top.swkfk.compiler.llvm.IrModule;
import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.llvm.value.Function;
import top.swkfk.compiler.llvm.value.GlobalVariable;
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
    private final List<MipsGlobalVariable> globalVariable = new LinkedList<>();
    private final Map<Function, MipsFunction> functionMap = new HashMap<>();

    @Override
    public ArchModule runParseIr(IrModule module) {
        module.getGlobalVariables().forEach(this::parseGlobalVariable);
        module.getFunctions().stream().filter(function -> !function.isExternal()).forEach(this::parseFunction);
        return this;
    }

    private void parseGlobalVariable(GlobalVariable globalVariable) {
        this.globalVariable.add(new MipsGlobalVariable(
            globalVariable.getType(),
            globalVariable.getName() + ".addr",
            globalVariable.getInitializerList()
        ));
    }

    private void parseFunction(Function function) {
        MipsBlock entry = new MipsBlock(function.getName() + ".entry");
        MipsBlock exit = new MipsBlock(function.getName() + ".exit");

        MipsGenerator generator = new MipsGenerator(function.getBlocks(), functionMap, entry, exit);
        MipsFunction mipsFunction = new MipsFunction(function.getName());
        mipsFunction.setKeyBlock(entry, exit);
        functions.add(mipsFunction);
        functionMap.put(function, mipsFunction);

        // Fill the entry block. Except for the sub $sp
        if (!function.getName().equals("main")) {
            entry.addInstruction(new MipsILoadStore(MipsILoadStore.X.sw, MipsPhysicalRegister.fp, MipsPhysicalRegister.sp, new MipsImmediate(-4)));
        }
        entry.addInstruction(new MipsIBinary(MipsIBinary.X.addiu, MipsPhysicalRegister.fp, MipsPhysicalRegister.sp, new MipsImmediate(0)));
        for (int i = 0; i < function.getParams().size(); i++) {
            generator.addParameter(function.getParams().get(i), i).forEach(entry::addInstruction);
        }

        mipsFunction.addBlock(entry);
        for (DualLinkedList.Node<BasicBlock> node : function.getBlocks()) {
            parseBlock(node.getData(), mipsFunction, generator);
        }

        mipsFunction.addBlock(exit);
        mipsFunction.setStackSize(generator.getStackSize());
        if (!function.getName().equals("main")) {
            exit.addInstruction(new MipsILoadStore(MipsILoadStore.X.lw, MipsPhysicalRegister.fp, MipsPhysicalRegister.fp, new MipsImmediate(-4)));
        }
        exit.addInstruction(new MipsIJump(MipsIJump.X.jr, MipsPhysicalRegister.ra));
    }

    private void parseBlock(BasicBlock block, MipsFunction mipsFunction, MipsGenerator generator) {
        MipsBlock mipsBlock = generator.blockLLVM2Mips(block);
        mipsFunction.addBlock(mipsBlock);
        block.getInstructions().forEach(node -> parseInstruction(node.getData(), mipsBlock, generator));
    }

    private void parseInstruction(User instruction, MipsBlock mipsBlock, MipsGenerator generator) {
        mipsBlock.reservedComment = instruction.toLLVM();
        generator.preRun(mipsBlock, instruction, globalVariable).forEach(mipsBlock::addInstruction);
        generator.run(mipsBlock, instruction).forEach(mipsBlock::addInstruction);
        mipsBlock.reservedComment = null;  // Maybe there are no instruction added
    }

    @Override
    public ArchModule runRemovePhi() {
        functions.forEach(function -> new MipsFunctionRemovePhi(function).run());
        return this;
    }

    @Override
    public ArchModule runVirtualOptimize() {
        return this;
    }

    @Override
    public ArchModule runRegisterAllocation() {
        functions.forEach(function -> new MipsFunctionRegisterAllocate(function)
            .runCheckAllocateStrategy()
            .runAllocateTemporaryRegisters()
            .runAllocateGlobalRegisters()
            .refill()
        );
        functions.forEach(MipsFunction::fillStackSize);
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

        for (var global : globalVariable) {
            if (Configure.debug.displayDataSegment) {
                System.err.println(global);
            }
            sb.append("  ").append(global.toMips()).append("\n");
        }

        sb.append("\n.text\n\n");
        sb.append("    jal main.entry\n");
        sb.append("    li $v0, 10\n");
        sb.append("    syscall\n\n");

        for (MipsFunction function : functions) {
            sb.append("# Function: ").append(function).append("\n");
            sb.append(function.toMips()).append("\n");
        }

        return sb.toString();
    }
}
