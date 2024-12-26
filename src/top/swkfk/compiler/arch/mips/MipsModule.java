package top.swkfk.compiler.arch.mips;

import top.swkfk.compiler.Configure;
import top.swkfk.compiler.arch.ArchModule;
import top.swkfk.compiler.arch.mips.instruction.MipsIBinary;
import top.swkfk.compiler.arch.mips.instruction.MipsIJump;
import top.swkfk.compiler.arch.mips.instruction.MipsILoadStore;
import top.swkfk.compiler.arch.mips.operand.MipsImmediate;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.optimize.Peephole;
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

final public class MipsModule implements ArchModule {
    private final List<MipsFunction> functions = new LinkedList<>();
    private final List<MipsGlobalVariable> globalVariable = new LinkedList<>();
    private final Map<Function, MipsFunction> functionMap = new HashMap<>();
    private MipsFunction mainFunction;

    @Override
    public ArchModule runParseIr(IrModule module) {
        // 处理入口，依次处理全局变量和函数
        module.getGlobalVariables().forEach(this::parseGlobalVariable);
        module.getFunctions().stream().filter(function -> !function.isExternal()).forEach(this::parseFunction);
        return this;
    }

    public List<MipsFunction> getFunctions() {
        return functions;
    }

    private void parseGlobalVariable(GlobalVariable globalVariable) {
        this.globalVariable.add(new MipsGlobalVariable(
            globalVariable.getType(),
            globalVariable.getName() + ".addr",  // Magic 命名，用于获取全局变量的地址
            globalVariable.getInitializerList()
        ));
    }

    private void parseFunction(Function function) {
        // 新建一个函数的入口和出口的基本块，返回操作只在出口块中，参数操作只在入口块中
        MipsBlock entry = new MipsBlock(function.getName() + ".entry");
        MipsBlock exit = new MipsBlock(function.getName() + ".exit");

        // MipsGenerator 用于将 LLVM IR 转换为 MIPS 汇编，每个函数一个
        // functionMap 用于存储 LLVM 函数和 MIPS 函数的映射关系，动态添加，因为先声明后使用
        MipsGenerator generator = new MipsGenerator(function.getBlocks(), functionMap, entry, exit);
        // 新建一个函数，并将入口和出口块设置为关键块
        MipsFunction mipsFunction = new MipsFunction(function.getName());
        mipsFunction.setKeyBlock(entry, exit);
        // 添加至函数列表中
        functions.add(mipsFunction);
        functionMap.put(function, mipsFunction);

        // Fill the entry block. Except for the sub $sp
        // 存储调用者的 $fp，main 函数不需要
        if (!function.getName().equals("main")) {
            entry.addInstruction(new MipsILoadStore(MipsILoadStore.X.sw, MipsPhysicalRegister.fp, MipsPhysicalRegister.sp, new MipsImmediate(-4)));
        }
        // 设置 $fp 为调用者的 $sp，也就是当前的栈底
        entry.addInstruction(new MipsIBinary(MipsIBinary.X.addiu, MipsPhysicalRegister.fp, MipsPhysicalRegister.sp, new MipsImmediate(0)));
        // 提取参数至虚拟寄存器，生成对应的指令
        for (int i = 0; i < function.getParams().size(); i++) {
            generator.addParameter(function.getParams().get(i), i).forEach(entry::addInstruction);
        }

        // 将入口块添加至函数中，然后依次处理每个基本块
        mipsFunction.addBlock(entry);
        for (DualLinkedList.Node<BasicBlock> node : function.getBlocks()) {
            parseBlock(node.getData(), mipsFunction, generator);
        }
        // 将出口块添加至函数中
        mipsFunction.addBlock(exit);

        // 此时，可以初步确定函数的栈大小，但是只是保存一下，并不生成指令
        // 注意，寄存器分配后，栈大小可能会变化，这里只有参数与局部变量的大小
        mipsFunction.setStackSize(generator.getStackSize());
        // 恢复 $fp，main 函数不需要
        if (!function.getName().equals("main")) {
            exit.addInstruction(new MipsILoadStore(MipsILoadStore.X.lw, MipsPhysicalRegister.fp, MipsPhysicalRegister.fp, new MipsImmediate(-4)));
        }
        // 返回操作
        exit.addInstruction(new MipsIJump(MipsIJump.X.jr, MipsPhysicalRegister.ra));

        // 设置 main 函数
        if (function.getName().equals("main")) {
            mainFunction = mipsFunction;
        }
    }

    private void parseBlock(BasicBlock block, MipsFunction mipsFunction, MipsGenerator generator) {
        // 从 LLVM 基本块生成 MIPS 基本块，这是因为，MipsGenerator 在构造时，会遍历 LLVM 基本块，并将其转换为 MIPS 基本块
        MipsBlock mipsBlock = generator.blockLLVM2Mips(block);
        // 将 MIPS 基本块添加至 MIPS 函数中
        mipsFunction.addBlock(mipsBlock);
        // 遍历 LLVM 基本块的指令，生成 MIPS 指令
        block.getInstructions().forEach(node -> parseInstruction(node.getData(), mipsBlock, generator));
    }

    private void parseInstruction(User instruction, MipsBlock mipsBlock, MipsGenerator generator) {
        // 将 LLVM 指令作为注释，添加至 MIPS 指令中
        mipsBlock.reservedComment = instruction.toLLVM();
        // preRun，正式转换之前执行，可能会生成一些指令，目前主要是用于合并字符输出
        generator.preRun(mipsBlock, instruction, globalVariable).forEach(mipsBlock::addInstruction);
        // 正式转换，生成 MIPS 指令
        generator.run(mipsBlock, instruction).forEach(mipsBlock::addInstruction);
        mipsBlock.reservedComment = null;  // Maybe there are no instruction added
    }

    @Override
    public ArchModule runRemovePhi() {
        // 针对每个函数，移除 phi 指令
        functions.forEach(function -> new MipsFunctionRemovePhi(function).run());
        return this;
    }

    @Override
    public ArchModule runVirtualOptimize() {
        return this;
    }

    @Override
    public ArchModule runRegisterAllocation() {
        // 针对每个函数，进行寄存器分配
        functions.forEach(function -> new MipsFunctionRegisterAllocate(function)
            // 扫描寄存器分配策略：块内活跃（不跨函数调用），临时寄存器；块间活跃（跨函数调用），全局寄存器
            .runCheckAllocateStrategy()
            // 线性扫描分配临时寄存器
            .runAllocateTemporaryRegisters()
            // 图着色分配全局寄存器
            .runAllocateGlobalRegisters()
            // 替换寄存器，增加栈操作
            .refill()
        );
        // 针对每个函数，填充栈大小
        functions.forEach(MipsFunction::fillStackSize);
        return this;
    }

    @Override
    public ArchModule runPhysicalOptimize() {
        new Peephole().run(this);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Module: ").append(Configure.source).append("\n");

        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        sb.append("# Compiled at: ").append(time).append("\n");

        // 数据段，全局变量
        sb.append("\n.data\n");

        for (var global : globalVariable) {
            if (Configure.debug.displayDataSegment) {
                System.err.println(global);
            }
            sb.append("  ").append(global.toMips()).append("\n");
        }

        // 代码段，main 函数在前，其他函数在后，main 函数直接进入
        sb.append("\n.text\n\n");
        sb.append("#### main ####\n");
        sb.append(mainFunction.toMips()).append("\n");

        // main.exit 块会进行特殊处理，增加 syscall 指令退出程序

        // 生成其他函数的代码
        for (MipsFunction function : functions) {
            if (function == mainFunction) {
                continue;
            }
            sb.append("# Function: ").append(function).append("\n");
            sb.append(function.toMips()).append("\n");
        }

        return sb.toString();
    }
}
