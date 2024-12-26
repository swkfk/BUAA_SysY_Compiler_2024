package top.swkfk.compiler.arch.mips.process;

import top.swkfk.compiler.arch.mips.MipsBlock;
import top.swkfk.compiler.arch.mips.MipsFunction;
import top.swkfk.compiler.arch.mips.MipsGlobalVariable;
import top.swkfk.compiler.arch.mips.instruction.MipsIBinary;
import top.swkfk.compiler.arch.mips.instruction.MipsIBitShift;
import top.swkfk.compiler.arch.mips.instruction.MipsIBrEqu;
import top.swkfk.compiler.arch.mips.instruction.MipsIHiLo;
import top.swkfk.compiler.arch.mips.instruction.MipsIJump;
import top.swkfk.compiler.arch.mips.instruction.MipsILoadAddress;
import top.swkfk.compiler.arch.mips.instruction.MipsILoadStore;
import top.swkfk.compiler.arch.mips.instruction.MipsIMultDiv;
import top.swkfk.compiler.arch.mips.instruction.MipsIPhi;
import top.swkfk.compiler.arch.mips.instruction.MipsISyscall;
import top.swkfk.compiler.arch.mips.instruction.MipsIUnimp;
import top.swkfk.compiler.arch.mips.instruction.MipsInstruction;
import top.swkfk.compiler.arch.mips.operand.MipsImmediate;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.operand.MipsVirtualRegister;
import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.frontend.symbol.type.Ty;
import top.swkfk.compiler.frontend.symbol.type.TyArray;
import top.swkfk.compiler.frontend.symbol.type.TyPtr;
import top.swkfk.compiler.helpers.GlobalCounter;
import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.llvm.value.Function;
import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.llvm.value.constants.ConstInteger;
import top.swkfk.compiler.llvm.value.instruction.BinaryOp;
import top.swkfk.compiler.llvm.value.instruction.IAllocate;
import top.swkfk.compiler.llvm.value.instruction.IBinary;
import top.swkfk.compiler.llvm.value.instruction.IBranch;
import top.swkfk.compiler.llvm.value.instruction.ICall;
import top.swkfk.compiler.llvm.value.instruction.IComparator;
import top.swkfk.compiler.llvm.value.instruction.IConvert;
import top.swkfk.compiler.llvm.value.instruction.IGep;
import top.swkfk.compiler.llvm.value.instruction.ILoad;
import top.swkfk.compiler.llvm.value.instruction.IMove;
import top.swkfk.compiler.llvm.value.instruction.IPhi;
import top.swkfk.compiler.llvm.value.instruction.IReturn;
import top.swkfk.compiler.llvm.value.instruction.IStore;
import top.swkfk.compiler.utils.DualLinkedList;
import top.swkfk.compiler.utils.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/// Convention:
/// <code>$a0</code> ~ <code>$a3</code>: The first four arguments
///
/// <pre>
/// |                                        |
/// |           Caller Stack Frame           |
/// |                                        |
/// +========================================+  <-- Caller $sp == Callee $fp
/// |              Caller's $fp              |
/// +----------------------------------------+  <-- Caller $sp - 4
/// |            The 5th Argument            |
/// +----------------------------------------+  <-- Caller $sp - 8
/// |            The 6th Argument            |
/// +----------------------------------------+  <-- Caller $sp - 12
/// |             Other Argument             |
/// +----------------------------------------+
/// |             Local Variable             |
/// +----------------------------------------+
/// |           Spilled registers            |
/// +----------------------------------------+
/// |           Reserved register            |
/// +----------------------------------------+  <-- Callee $sp
/// </pre>
///
final public class MipsGenerator {
    private final Map<Value, MipsVirtualRegister> valueMap = new HashMap<>();
    private final Map<BasicBlock, MipsBlock> blockMap = new HashMap<>();
    private final Map<Function, MipsFunction> functionMap;
    private final MipsBlock exitBlock;
    private int stackSize;

    /**
     * 构造函数，里面会初始化很多内容
     * @param blocks 函数的全部基本块
     * @param functionMap 函数映射（LLVM -> MIPS）
     * @param entry 函数的入口块
     * @param exit 函数的出口块
     */
    public MipsGenerator(DualLinkedList<BasicBlock> blocks, Map<Function, MipsFunction> functionMap, MipsBlock entry, MipsBlock exit) {
        // 将所有的基本块转换为 MIPS 基本块，并添加到 blockMap 中
        blocks.forEach(node -> blockMap.put(node.getData(), new MipsBlock(node.getData())));

        this.functionMap = functionMap;
        this.exitBlock = exit;

        // 入口块到第一个基本块的边
        MipsBlock.addEdge(entry, blockLLVM2Mips(blocks.getHead().getData()));
        // This will be added in the Return instruction
        // MipsBlock.addEdge(blockLLVM2Mips(blocks.getTail().getData()), exit);

        // 构建 valueMap，遍历全部指令，将指令和虚拟寄存器对应起来
        buildValueMap();

        // 初始化栈大小
        if (entry.isMainEntry()) {
            stackSize = 0;
        } else {
            stackSize = 4;  // Initial size for caller's $fp
        }
    }

    /**
     * 将 LLVM 指令（User）与虚拟寄存器（MipsVirtualRegister）对应起来
     */
    private void buildValueMap() {
        for (BasicBlock block : blockMap.keySet()) {
            for (DualLinkedList.Node<User> iNode : block.getInstructions()) {
                User instruction = iNode.getData();
                if (instruction.getType() != null) {
                    valueMap.put(instruction, new MipsVirtualRegister());
                }
            }
        }
    }

    public MipsBlock blockLLVM2Mips(BasicBlock block) {
        return blockMap.get(block);
    }

    /**
     * 函数参数在栈中的偏移
     * @param index 参数序号，从 0 开始
     * @return 参数在栈中的偏移
     */
    private static int argumentOffset(int index) {
        assert index >= 4;  // 这里写的不好，应该用 MipsPhysicalRegister.a.length，而非 magic number
        return (2 + index - MipsPhysicalRegister.a.length) * -4;
    }

    /**
     * 添加参数，将其转化为 Mips 指令
     * @param value 参数的 LLVM IR 表示
     * @param index 参数序号，从 0 开始
     * @return 为了存储参数，需要（向入口块）添加的 Mips 指令列表
     */
    public List<MipsInstruction> addParameter(Value value, int index) {
        MipsVirtualRegister register = new MipsVirtualRegister();
        valueMap.put(value, register);
        if (index >= MipsPhysicalRegister.a.length) {
            enlargeStack(4);
            return List.of(
                new MipsILoadStore(MipsILoadStore.X.lw, register, MipsPhysicalRegister.fp, new MipsImmediate(argumentOffset(index)))
            );
        } else {
            return List.of(
                new MipsIBinary(MipsIBinary.X.addiu, register, MipsPhysicalRegister.a[index], new MipsImmediate(0))
            );
        }
    }

    // 这里是为了合并 LLVM 产生的连续的字符输出，合并为字符串
    private StringBuilder reservedOutputString = new StringBuilder();
    private static final GlobalCounter stringDataCounter = new GlobalCounter();
    private static final HashMap<String, String> strings = new HashMap<>();

    @SuppressWarnings("SpellCheckingInspection")
    public List<MipsInstruction> preRun(MipsBlock ignore, User instruction, List<MipsGlobalVariable> globalVariable) {
        if (instruction instanceof ICall call) {
            // 这种是需要存储的，交由后面处理
            if (call.getFunction().getName().equals("putch") && call.getOperand(0) instanceof ConstInteger) {
                return List.of();
            }
        }
        // 非 putch 指令，需要检查先前存储的字符输出内容
        if (reservedOutputString.isEmpty()) {
            // 为空，直接返回
            return List.of();
        }
        String content = reservedOutputString.toString();
        String tag;
        // 这里合并了相同的字符串
        if (strings.containsKey(content)) {
            tag = strings.get(content);
        } else {
            // 新建一个全局字符串变量
            tag = "str." + stringDataCounter.get();
            strings.put(content, tag);
            globalVariable.add(new MipsGlobalVariable(
                Ty.I8, tag, content
            ));
        }
        // 清空缓存
        reservedOutputString = new StringBuilder();
        // 通过 syscall 输出字符串
        return List.of(
            new MipsILoadAddress(new MipsImmediate(tag), MipsPhysicalRegister.a[0]),
            new MipsIBinary(MipsIBinary.X.addiu, MipsPhysicalRegister.v0, MipsPhysicalRegister.zero, new MipsImmediate(4)),
            new MipsISyscall()
        );
    }

    @SuppressWarnings("SpellCheckingInspection")
    public List<MipsInstruction> run(MipsBlock currentBlock, User instruction) {
        if (instruction instanceof IAllocate allocate) {
            // alloca 指令，扩充栈空间，注意对齐需求
            int offset = enlargeStack((((TyPtr) allocate.getType()).getBase().sizeof() + 3) & ~0b0011);
            // 地址存在指针中，现在是一个虚拟寄存器，所以不需要在其他地方记录，这种写法的坏处是，寄存器分配的压力很大！
            MipsVirtualRegister register = valueMap.get(allocate);
            return List.of(
                new MipsIBinary(MipsIBinary.X.addiu, register, MipsPhysicalRegister.fp, new MipsImmediate(offset))
            );
        }
        if (instruction instanceof IBinary || instruction instanceof IComparator) {
            // 二元指令（包括计算与比较）
            BinaryOp opcode;
            if (instruction instanceof IBinary) {
                opcode = ((IBinary) instruction).getOpcode();
            } else {
                opcode = ((IComparator) instruction).getOpcode();
            }
            // 这里处理一下两个操作数都是常数的情况，直接计算结果
            if (Value.allConstInteger(instruction.getOperand(0), instruction.getOperand(1))) {
                MipsVirtualRegister register = valueMap.get(instruction);
                return List.of(new MipsIBinary(
                    MipsIBinary.X.addiu, register, MipsPhysicalRegister.zero,
                    new MipsImmediate(opcode.calculate(
                        ((ConstInteger) instruction.getOperand(0)).getValue(),
                        ((ConstInteger) instruction.getOperand(1)).getValue()
                    ))
                ));
            }
            // 下面，根据 opcode 生成对应的 MIPS 指令
            return switch (opcode) {
                case ADD -> buildBinaryHelperChooseI(
                    instruction, MipsIBinary.X.addu, MipsIBinary.X.addiu
                );
                case SUB -> MipsInstructionHub.sub(instruction, valueMap);
                case MUL -> buildMultDivHelper(
                    instruction, MipsIHiLo.X.mflo, MipsIMultDiv.X.mult
                );
                // 除法和取模，需要特殊处理，直接生成快速的算法
                case DIV -> {
                    assert instruction instanceof IBinary;
                    yield buildFastDivide((IBinary) instruction, MipsIHiLo.X.mflo);
                }
                case MOD -> {
                    assert instruction instanceof IBinary;
                    yield buildFastDivide((IBinary) instruction, MipsIHiLo.X.mfhi);
                }
                case AND -> buildBinaryHelperChooseI(
                    instruction, MipsIBinary.X.and, MipsIBinary.X.andi
                );
                case OR -> buildBinaryHelperChooseI(
                    instruction, MipsIBinary.X.or, MipsIBinary.X.ori
                );
                case XOR -> buildBinaryHelperChooseI(
                    instruction, MipsIBinary.X.xor, MipsIBinary.X.xori
                );
                case SHL -> buildShiftHelper(
                    instruction, MipsIBitShift.X.sllv, MipsIBitShift.X.sll
                );
                case SHR -> buildShiftHelper(
                    instruction, MipsIBitShift.X.srav, MipsIBitShift.X.sra
                );
                case Separator -> throw new RuntimeException("Separator is not a valid binary operator");
                case Eq -> buildCompareHelper(instruction, MipsIBinary.X.seq);
                case Ne -> buildCompareHelper(instruction, MipsIBinary.X.sne);
                case Lt -> buildCompareHelper(instruction, MipsIBinary.X.slt);
                case Le -> buildCompareHelper(instruction, MipsIBinary.X.sle);
                case Gt -> buildCompareHelper(instruction, MipsIBinary.X.sgt);
                case Ge -> buildCompareHelper(instruction, MipsIBinary.X.sge);
            };
        }
        if (instruction instanceof IBranch branch) {
            if (branch.isConditional()) {
                // 条件跳转
                Pair<BasicBlock, BasicBlock> target = branch.getConditionalTarget();
                // 维护基本块之间的边
                MipsBlock.addEdge(currentBlock, blockLLVM2Mips(target.first()));
                MipsBlock.addEdge(currentBlock, blockLLVM2Mips(target.second()));
                if (branch.getOperand(0) instanceof ConstInteger integer) {
                    // 跳转条件是立即数，需要先加载到寄存器中
                    MipsVirtualRegister register = new MipsVirtualRegister();
                    return List.of(
                        new MipsIBinary(MipsIBinary.X.addiu, register, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())),
                        new MipsIBrEqu(MipsIBrEqu.X.bne, register, MipsPhysicalRegister.zero, blockLLVM2Mips(target.first())),
                        new MipsIJump(MipsIJump.X.j, blockLLVM2Mips(target.second()))
                    );
                } else {
                    // 跳转条件是寄存器，直接比较即可
                    MipsVirtualRegister cond = valueMap.get(branch.getOperand(0));
                    return List.of(
                        new MipsIBrEqu(MipsIBrEqu.X.bne, cond, MipsPhysicalRegister.zero, blockLLVM2Mips(target.first())),
                        new MipsIJump(MipsIJump.X.j, blockLLVM2Mips(target.second()))
                    );
                }
            } else {
                // 无条件跳转，维护一下基本块之间的边，然后生成跳转指令
                MipsBlock.addEdge(currentBlock, blockLLVM2Mips(branch.getTarget()));
                return List.of(new MipsIJump(MipsIJump.X.j, blockLLVM2Mips(branch.getTarget())));
            }
        }
        if (instruction instanceof ICall call) {
            // 需要翻译为 syscall 的函数调用
            // 注意，函数调用可能会有参数，也会有返回值，这里通过 buildSyscallWrite 和 buildSyscallRead 来实现
            // Read 而来的参数直接存储在 LLVM call 指令的 Value 中，Write 的参数可能需要先从各个地方读取
            if (call.getFunction().getName().equals("putch") && call.getOperand(0) instanceof ConstInteger integer) {
                // 输出一个字符常数，存储在缓存中，等待后续处理
                reservedOutputString.append((char) integer.getValue());
                return List.of();
            } else if (call.getFunction().getName().equals("putch")) {
                // 输出字符，syscall no. 11
                return MipsInstructionHub.buildSyscallWrite(11, call.getOperand(0), valueMap);
            } else if (call.getFunction().getName().equals("putint")) {
                // 输出整数，syscall no. 1
                return MipsInstructionHub.buildSyscallWrite(1, call.getOperand(0), valueMap);
            } else if (call.getFunction().getName().equals("getint")) {
                // 读取整数，syscall no. 5
                MipsVirtualRegister register = valueMap.get(call);
                return MipsInstructionHub.buildSyscallRead(5, register);
            } else if (call.getFunction().getName().equals("getchar")) {
                // 读取字符，syscall no. 12
                MipsVirtualRegister register = valueMap.get(call);
                return MipsInstructionHub.buildSyscallRead(12, register);
            }

            // 拿一下目标函数的入口块，这里不需要维护边，因为这是一个函数调用，不是基本块之间的跳转
            // 跳转的对象是函数的入口块，而不是“函数”本身
            MipsBlock target = functionMap.get(call.getFunction()).getEntryBlock();
            // MipsBlock.addEdge(currentBlock, target);

            // 处理函数参数，将参数存储到对应的寄存器或栈中
            List<MipsInstruction> list = new LinkedList<>();
            for (int i = 0; i < call.getOperands().size() && i < MipsPhysicalRegister.a.length; i++) {
                // 前四个参数存储在 $a0 ~ $a3 中
                list.add(new MipsIBinary(
                    MipsIBinary.X.addiu, MipsPhysicalRegister.a[i],
                    eliminateImmediate(list, call.getOperand(i)), new MipsImmediate(0)
                ));
            }
            for (int i = MipsPhysicalRegister.a.length; i < call.getOperands().size(); i++) {
                // 将参数存储在（被调用函数的）栈中，立即数的处理在 eliminateImmediate 中，会存储在某个虚拟寄存器中
                list.add(new MipsILoadStore(MipsILoadStore.X.sw,
                    eliminateImmediate(list, call.getOperand(i)),
                    MipsPhysicalRegister.sp, new MipsImmediate(argumentOffset(i))
                ));
            }
            // 生成调用函数的跳转指令（jal）
            list.add(new MipsIJump(MipsIJump.X.jal, target));
            // 处理返回值，如果有的话
            if (call.getType() != Ty.Void) {
                MipsVirtualRegister register = valueMap.get(call);
                list.add(new MipsIBinary(MipsIBinary.X.addiu, register, MipsPhysicalRegister.v0, new MipsImmediate(0)));
            }
            return list;
        }
        if (instruction instanceof IReturn ret) {
            // 返回指令，统一跳转到出口块
            MipsBlock.addEdge(currentBlock, exitBlock);

            if (ret.getOperands().isEmpty()) {
                // 这里是 void 类型的返回，直接跳转到出口块
                return List.of(new MipsIJump(MipsIJump.X.j, exitBlock));
            }
            // 有返回值（立即数），需要将返回值存储到 v0 中
            if (ret.getOperand(0) instanceof ConstInteger integer) {
                return List.of(
                    new MipsIBinary(MipsIBinary.X.addiu, MipsPhysicalRegister.v0, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())),
                    new MipsIJump(MipsIJump.X.j, exitBlock)
                );
            }
            // 返回值存储在某个寄存器中，直接将其移动到 v0 中
            return List.of(
                new MipsIBinary(MipsIBinary.X.addiu, MipsPhysicalRegister.v0, valueMap.get(ret.getOperand(0)), new MipsImmediate(0)),
                new MipsIJump(MipsIJump.X.j, exitBlock)
            );
        }
        if (instruction instanceof ILoad load) {
            // 加载指令，地址全部都用虚拟寄存器表示
            MipsVirtualRegister register = valueMap.get(load);
            MipsILoadStore.X opcode;
            if (load.getType().sizeof() == 1) {
                // char 是 unsigned，因此使用 lbu
                opcode = MipsILoadStore.X.lbu;
            } else if (load.getType().sizeof() == 2) {
                // 实际不会遇到
                opcode = MipsILoadStore.X.lhu;
            } else if (load.getType().sizeof() == 4) {
                opcode = MipsILoadStore.X.lw;
            } else {
                throw new RuntimeException("Unsupported load size");
            }
            // 对全局变量的特殊处理，加载 tag 即可
            if (load.getOperand(0).getName().startsWith("@")) {
                return List.of(new MipsILoadStore(
                    opcode, register, MipsPhysicalRegister.zero,
                    new MipsImmediate(globalValueToMipsTag(load.getOperand(0)))
                ));
            }
            return List.of(new MipsILoadStore(opcode, register, valueMap.get(load.getOperand(0)), new MipsImmediate(0)));
        }
        if (instruction instanceof IStore store) {
            // 存储指令
            List<MipsInstruction> res = new LinkedList<>();
            MipsOperand operand;
            MipsILoadStore.X opcode;
            if (store.getOperand(0).getType().sizeof() == 1) {
                opcode = MipsILoadStore.X.sb;
            } else if (store.getOperand(0).getType().sizeof() == 2) {
                opcode = MipsILoadStore.X.sh;
            } else if (store.getOperand(0).getType().sizeof() == 4) {
                opcode = MipsILoadStore.X.sw;
            } else {
                throw new RuntimeException("Unsupported load size");
            }
            if (store.getOperand(0) instanceof ConstInteger integer) {
                // 存储的是常数，需要先搞一个虚拟寄存器
                operand = new MipsVirtualRegister();
                res.add(new MipsIBinary(MipsIBinary.X.addiu, operand, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())));
            } else {
                operand = valueMap.get(store.getOperand(0));
            }
            if (store.getOperand(1).getName().startsWith("@")) {
                res.add(new MipsILoadStore(
                    opcode, operand, MipsPhysicalRegister.zero,
                    new MipsImmediate(globalValueToMipsTag(store.getOperand(1)))
                ));
            } else {
                res.add(new MipsILoadStore(
                    opcode, operand, valueMap.get(store.getOperand(1)), new MipsImmediate(0)
                ));
            }
            return res;
        }
        if (instruction instanceof IMove move) {
            // XXX: Unchecked!
            // 应该没有用到
            if (move.getOperand(0) instanceof ConstInteger integer) {
                MipsVirtualRegister register = valueMap.get(move);
                return List.of(new MipsIBinary(MipsIBinary.X.addiu, register, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())));
            }
            valueMap.put(move, valueMap.get(move.getOperand(0)));
            return List.of();
        }
        if (instruction instanceof IConvert convert) {
            // 类型转换指令，根据位宽变化执行相应的指令
            MipsVirtualRegister register = valueMap.get(convert);
            if (convert.isTruncating()) {
                // 截断，通过 andi 进行，如果是立即数，在编译器中完成结果计算
                if (convert.getOperand(0) instanceof ConstInteger integer) {
                    return List.of(new MipsIBinary(MipsIBinary.X.addiu, register, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue() % (1 << convert.getType().sizeof() * 8))));
                } else {
                    return List.of(new MipsIBinary(MipsIBinary.X.andi, register, valueMap.get(convert.getOperand(0)), new MipsImmediate((1 << convert.getType().sizeof() * 8) - 1)));
                }
            } else {
                // 无符号拓展，直接通过 addiu 完成
                if (convert.getOperand(0) instanceof ConstInteger integer) {
                    return List.of(new MipsIBinary(MipsIBinary.X.addiu, register, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())));
                } else {
                    return List.of(new MipsIBinary(MipsIBinary.X.addiu, register, valueMap.get(convert.getOperand(0)), new MipsImmediate(0)));
                }
            }
        }
        if (instruction instanceof IPhi phi) {
            // phi 指令，等待后续 remove phi 操作
            MipsVirtualRegister register = valueMap.get(phi);
            MipsIPhi mipsPhi = new MipsIPhi(register);
            for (Pair<BasicBlock, Value> pair : phi.getIncoming()) {
                // 这里遍历每一个 income，将其添加到 phi 指令中，没有过多的处理
                MipsOperand operand;
                if (pair.second() instanceof ConstInteger integer) {
                    operand = new MipsImmediate(integer.getValue());
                } else {
                    operand = valueMap.get(pair.second());
                }
                mipsPhi.addOperand(operand, blockLLVM2Mips(pair.first()));
            }
            return List.of(mipsPhi);
        }
        if (instruction instanceof IGep gep) {
            // gep 指令，计算地址，需要处理一些特殊情况
            List<MipsInstruction> res = new LinkedList<>();
            MipsVirtualRegister register = valueMap.get(gep);
            Value pointer = gep.getOperand(0);

            // 得到基地址的寄存器
            MipsOperand pointerOperand;
            if (pointer.getName().startsWith("@")) {
                // 全局变量，首先加载基地址
                pointerOperand = new MipsVirtualRegister();
                res.add(new MipsILoadAddress(
                    new MipsImmediate(globalValueToMipsTag(pointer)),
                    pointerOperand
                ));
            } else {
                pointerOperand = valueMap.get(pointer);
            }

            Value offset = gep.getOperand(1);
            SymbolType base;
            if (gep.isFromArgument()) {
                // 从参数来的，基础类型直接脱一层指针
                base = ((TyPtr) pointer.getType()).getBase();
            } else {
                // 分配来的，基础类型需要脱一层指针，还要再脱一层数组
                base = ((TyArray) ((TyPtr) pointer.getType()).getBase()).getBase();
            }

            if (offset instanceof ConstInteger integer) {
                // 偏移是常数，通过 addiu 计算，具体的偏移量在编译器中计算
                res.add(new MipsIBinary(
                    MipsIBinary.X.addiu, register, pointerOperand,
                    new MipsImmediate(integer.getValue() * base.sizeof()))
                );
            } else {
                // 偏移是寄存器，通过 sll 计算实际偏移
                MipsVirtualRegister temp = new MipsVirtualRegister();
                res.addAll(List.of(
                    new MipsIBitShift(
                        MipsIBitShift.X.sll, temp, valueMap.get(offset),
                        // ceil(log2(x)) = 32 - numberOfLeadingZeros(x - 1)
                        new MipsImmediate(32 - Integer.numberOfLeadingZeros(base.sizeof() - 1))
                        // 这里其实有点问题，如果是字符数组，会生成一条多余的指令，即 sll $temp, $offset, 0
                    ),
                    new MipsIBinary(MipsIBinary.X.addu, register, pointerOperand, temp)
                ));
            }
            return res;
        }
        return List.of(new MipsIUnimp());
    }

    public static String globalValueToMipsTag(Value pointer) {
        assert pointer.getName().startsWith("@");
        return pointer.getName().substring(1) + ".addr";
    }

    private int enlargeStack(int size) {
        stackSize += size;
        return -stackSize;
    }

    public int getStackSize() {
        return stackSize;
    }

    private List<MipsInstruction> buildCompareHelper(User binary, MipsIBinary.X opcode) {
        Value lhs = binary.getOperand(0);
        Value rhs = binary.getOperand(1);
        MipsVirtualRegister register = valueMap.get(binary);
        List<MipsInstruction> res = new LinkedList<>();
        MipsOperand lhsOperand, rhsOperand;

        // 这里简单处理了，立即数全部存入寄存器
        if (lhs instanceof ConstInteger integer) {
            lhsOperand = new MipsVirtualRegister();
            res.add(new MipsIBinary(MipsIBinary.X.addiu, lhsOperand, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())));
        } else {
            lhsOperand = valueMap.get(lhs);
        }
        if (rhs instanceof ConstInteger integer) {
            rhsOperand = new MipsVirtualRegister();
            res.add(new MipsIBinary(MipsIBinary.X.addiu, rhsOperand, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())));
        } else {
            rhsOperand = valueMap.get(rhs);
        }

        res.add(new MipsIBinary(opcode, register, lhsOperand, rhsOperand));
        return res;
    }

    /**
     * Choose the i-type or r-type instruction to generate the mips code.
     *
     * @param binary  binary instruction, the target value
     * @param opcode  r-type instruction
     * @param opcodeI i-type instruction
     * @return mips code list
     */
    private List<MipsInstruction> buildBinaryHelperChooseI(User binary, MipsIBinary.X opcode, MipsIBinary.X opcodeI) {
        return buildBinaryHelperChooseI(binary, opcode, opcodeI, binary.getOperand(0), binary.getOperand(1));
    }

    /**
     * Choose the i-type or r-type instruction to generate the mips code and specify the operand.
     * @param binary binary instruction, the target value
     * @param opcode r-type instruction
     * @param opcodeI i-type instruction
     * @param lhs left operand
     * @param rhs right operand
     * @return mips code list
     */
    private List<MipsInstruction> buildBinaryHelperChooseI(User binary, MipsIBinary.X opcode, MipsIBinary.X opcodeI, Value lhs, Value rhs) {
        if (lhs instanceof ConstInteger integer) {
            MipsVirtualRegister register = valueMap.get(binary);
            return List.of(new MipsIBinary(opcodeI, register, valueMap.get(rhs), new MipsImmediate(integer.getValue())));
        }
        if (rhs instanceof ConstInteger integer) {
            MipsVirtualRegister register = valueMap.get(binary);
            return List.of(new MipsIBinary(opcodeI, register, valueMap.get(lhs), new MipsImmediate(integer.getValue())));
        }
        MipsVirtualRegister register = valueMap.get(binary);
        return List.of(new MipsIBinary(opcode, register, valueMap.get(lhs), valueMap.get(rhs)));
    }

    /**
     * Build the mips code for the mult, div and mod.
     *
     * @param binary binary instruction, the target value
     * @param mf     from where to get the result
     * @param opcode mult or div
     * @return mips code list
     */
    @SuppressWarnings("SpellCheckingInspection")
    private List<MipsInstruction> buildMultDivHelper(User binary, MipsIHiLo.X mf, MipsIMultDiv.X opcode) {
        Value lhs = binary.getOperand(0);
        Value rhs = binary.getOperand(1);
        List<MipsInstruction> list = new LinkedList<>();
        MipsVirtualRegister resultRegister = valueMap.get(binary);
        if (lhs instanceof ConstInteger integer) {
            list.add(new MipsIBinary(MipsIBinary.X.addiu, MipsPhysicalRegister.at, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())));
            list.add(new MipsIMultDiv(opcode, MipsPhysicalRegister.at, valueMap.get(rhs)));
        } else if (rhs instanceof ConstInteger integer) {
            list.add(new MipsIBinary(MipsIBinary.X.addiu, MipsPhysicalRegister.at, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())));
            list.add(new MipsIMultDiv(opcode, valueMap.get(lhs), MipsPhysicalRegister.at));
        } else {
            list.add(new MipsIMultDiv(opcode, valueMap.get(lhs), valueMap.get(rhs)));
        }
        list.add(new MipsIHiLo(mf, resultRegister));
        return list;
    }

    private List<MipsInstruction> buildFastDivide(IBinary instruction, MipsIHiLo.X operator) {
        // 这里实际上只处理了取余，进行有限的优化
        if (!(instruction.getOperand(0) instanceof ConstInteger) && (instruction.getOperand(1) instanceof ConstInteger integer) && integer.getValue() != 0 && operator == MipsIHiLo.X.mfhi) {
            // 对 1 的取余，直接返回 0
            if (integer.getValue() == 1) {
                MipsVirtualRegister register = valueMap.get(instruction);
                return List.of(
                    new MipsIBinary(MipsIBinary.X.addu, register, MipsPhysicalRegister.zero, MipsPhysicalRegister.zero)
                );
            }
            // 对正数取余，并且是 2 的幂次方，可以通过位运算实现
            if (integer.getValue() > 0 && Integer.numberOfLeadingZeros(integer.getValue()) + Integer.numberOfTrailingZeros(integer.getValue()) == 31) {
                int k = Integer.numberOfTrailingZeros(integer.getValue());
                MipsOperand dividend = valueMap.get(instruction.getOperand(0));
                MipsVirtualRegister register = valueMap.get(instruction);
                MipsVirtualRegister fakeRes1 = new MipsVirtualRegister();
                MipsVirtualRegister fakeRes2 = new MipsVirtualRegister();
                MipsVirtualRegister fakeRes3 = new MipsVirtualRegister();
                MipsVirtualRegister temp1 = new MipsVirtualRegister();
                MipsVirtualRegister temp2 = new MipsVirtualRegister();
                // 参考：https://www.cnblogs.com/czlnb/p/15761065.html
                return List.of(
                    new MipsIBinary(MipsIBinary.X.andi, fakeRes1, dividend, new MipsImmediate(integer.getValue() - 1)),
                    new MipsIBinary(MipsIBinary.X.addiu, fakeRes2, fakeRes1, new MipsImmediate(-1)),
                    new MipsIBitShift(MipsIBitShift.X.sra, temp1, dividend, new MipsImmediate(31)),
                    new MipsIBitShift(MipsIBitShift.X.sll, temp2, temp1, new MipsImmediate(k)),
                    new MipsIBinary(MipsIBinary.X.or, fakeRes3, fakeRes2, temp2),
                    new MipsIBinary(MipsIBinary.X.addiu, register, fakeRes3, new MipsImmediate(1))
                );
            }
        }
        return buildMultDivHelper(instruction, operator, MipsIMultDiv.X.div);
    }

    private List<MipsInstruction> buildShiftHelper(User user, MipsIBitShift.X opcode, MipsIBitShift.X opcodeI) {
        List<MipsInstruction> list = new LinkedList<>();
        Value lhs = user.getOperand(0);
        Value rhs = user.getOperand(1);
        MipsOperand lhsOperand;
        if (lhs instanceof ConstInteger integer) {
            // 左操作数是立即数，需要先存储到寄存器中
            lhsOperand = new MipsVirtualRegister();
            list.add(new MipsIBinary(MipsIBinary.X.addiu, lhsOperand, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())));
        } else {
            lhsOperand = valueMap.get(lhs);
        }
        MipsVirtualRegister result = valueMap.get(user);
        if (rhs instanceof ConstInteger integer) {
            // 右操作数是立即数，直接生成对应的指令
            list.add(new MipsIBitShift(opcodeI, result, lhsOperand, new MipsImmediate(integer.getValue())));
        } else {
            list.add(new MipsIBitShift(opcode, result, lhsOperand, valueMap.get(rhs)));
        }
        return list;
    }

    private MipsOperand eliminateImmediate(List<MipsInstruction> instructions, Value value) {
        // 这里是为了处理立即数，将立即数存储到虚拟寄存器中，并返回这个虚拟寄存器
        if (value instanceof ConstInteger integer) {
            MipsVirtualRegister register = new MipsVirtualRegister();
            valueMap.put(value, register);
            instructions.add(new MipsIBinary(MipsIBinary.X.addiu, register, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())));
            return register;
        }
        return valueMap.get(value);
    }
}
