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

    public MipsGenerator(DualLinkedList<BasicBlock> blocks, Map<Function, MipsFunction> functionMap, MipsBlock entry, MipsBlock exit) {
        blocks.forEach(node -> blockMap.put(node.getData(), new MipsBlock(node.getData())));
        this.functionMap = functionMap;
        this.exitBlock = exit;
        MipsBlock.addEdge(entry, blockLLVM2Mips(blocks.getHead().getData()));
        // This will be added in the Return instruction
        // MipsBlock.addEdge(blockLLVM2Mips(blocks.getTail().getData()), exit);
        buildValueMap();
        if (entry.isMainEntry()) {
            stackSize = 0;
        } else {
            stackSize = 4;  // Initial size for caller's $fp
        }
    }

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

    private static int argumentOffset(int index) {
        assert index >= 5;
        return (2 + index - MipsPhysicalRegister.a.length) * -4;
    }

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

    private StringBuilder reservedOutputString = new StringBuilder();
    private static final GlobalCounter stringDataCounter = new GlobalCounter();
    private static final HashMap<String, String> strings = new HashMap<>();

    @SuppressWarnings("SpellCheckingInspection")
    public List<MipsInstruction> preRun(MipsBlock ignore, User instruction, List<MipsGlobalVariable> globalVariable) {
        if (instruction instanceof ICall call) {
            if (call.getFunction().getName().equals("putch") && call.getOperand(0) instanceof ConstInteger) {
                return List.of();
            }
        }
        if (reservedOutputString.isEmpty()) {
            return List.of();
        }
        String content = reservedOutputString.toString();
        String tag;
        if (strings.containsKey(content)) {
            tag = strings.get(content);
        } else {
            tag = "str." + stringDataCounter.get();
            strings.put(content, tag);
            globalVariable.add(new MipsGlobalVariable(
                Ty.I8, tag, content
            ));
        }
        reservedOutputString = new StringBuilder();
        return List.of(
            new MipsILoadAddress(new MipsImmediate(tag), MipsPhysicalRegister.a[0]),
            new MipsIBinary(MipsIBinary.X.addiu, MipsPhysicalRegister.v0, MipsPhysicalRegister.zero, new MipsImmediate(4)),
            new MipsISyscall()
        );
    }

    @SuppressWarnings("SpellCheckingInspection")
    public List<MipsInstruction> run(MipsBlock currentBlock, User instruction) {
        if (instruction instanceof IAllocate allocate) {
            int offset = enlargeStack((((TyPtr) allocate.getType()).getBase().sizeof() + 3) & ~0b0011);
            MipsVirtualRegister register = valueMap.get(allocate);
            return List.of(
                new MipsIBinary(MipsIBinary.X.addiu, register, MipsPhysicalRegister.fp, new MipsImmediate(offset))
            );
        }
        if (instruction instanceof IBinary || instruction instanceof IComparator) {
            BinaryOp opcode;
            if (instruction instanceof IBinary) {
                opcode = ((IBinary) instruction).getOpcode();
            } else {
                opcode = ((IComparator) instruction).getOpcode();
            }
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
            return switch (opcode) {
                case ADD -> buildBinaryHelperChooseI(
                    instruction, MipsIBinary.X.addu, MipsIBinary.X.addiu
                );
                case SUB -> MipsInstructionHub.sub(instruction, valueMap);
                case MUL -> buildMultDivHelper(
                    instruction, MipsIHiLo.X.mflo, MipsIMultDiv.X.mult
                );
                case DIV -> buildMultDivHelper(
                    instruction, MipsIHiLo.X.mflo, MipsIMultDiv.X.div
                );
                case MOD -> buildMultDivHelper(
                    instruction, MipsIHiLo.X.mfhi, MipsIMultDiv.X.div
                );
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
                Pair<BasicBlock, BasicBlock> target = branch.getConditionalTarget();
                MipsBlock.addEdge(currentBlock, blockLLVM2Mips(target.first()));
                MipsBlock.addEdge(currentBlock, blockLLVM2Mips(target.second()));
                if (branch.getOperand(0) instanceof ConstInteger integer) {
                    MipsVirtualRegister register = new MipsVirtualRegister();
                    return List.of(
                        new MipsIBinary(MipsIBinary.X.addiu, register, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())),
                        new MipsIBrEqu(MipsIBrEqu.X.bne, register, MipsPhysicalRegister.zero, blockLLVM2Mips(target.first())),
                        new MipsIJump(MipsIJump.X.j, blockLLVM2Mips(target.second()))
                    );
                } else {
                    MipsVirtualRegister cond = valueMap.get(branch.getOperand(0));
                    return List.of(
                        new MipsIBrEqu(MipsIBrEqu.X.bne, cond, MipsPhysicalRegister.zero, blockLLVM2Mips(target.first())),
                        new MipsIJump(MipsIJump.X.j, blockLLVM2Mips(target.second()))
                    );
                }
            } else {
                MipsBlock.addEdge(currentBlock, blockLLVM2Mips(branch.getTarget()));
                return List.of(new MipsIJump(MipsIJump.X.j, blockLLVM2Mips(branch.getTarget())));
            }
        }
        if (instruction instanceof ICall call) {
            // Special functions
            if (call.getFunction().getName().equals("putch") && call.getOperand(0) instanceof ConstInteger integer) {
                reservedOutputString.append((char) integer.getValue());
                return List.of();
            } else if (call.getFunction().getName().equals("putch")) {
                return MipsInstructionHub.buildSyscallWrite(11, call.getOperand(0), valueMap);
            } else if (call.getFunction().getName().equals("putint")) {
                return MipsInstructionHub.buildSyscallWrite(1, call.getOperand(0), valueMap);
            } else if (call.getFunction().getName().equals("getint")) {
                MipsVirtualRegister register = valueMap.get(call);
                return MipsInstructionHub.buildSyscallRead(5, register);
            } else if (call.getFunction().getName().equals("getchar")) {
                MipsVirtualRegister register = valueMap.get(call);
                return MipsInstructionHub.buildSyscallRead(12, register);
            }

            MipsBlock target = functionMap.get(call.getFunction()).getEntryBlock();
            // MipsBlock.addEdge(currentBlock, target);

            List<MipsInstruction> list = new LinkedList<>();
            for (int i = 0; i < call.getOperands().size() && i < MipsPhysicalRegister.a.length; i++) {
                list.add(new MipsIBinary(
                    MipsIBinary.X.addiu, MipsPhysicalRegister.a[i],
                    eliminateImmediate(list, call.getOperand(i)), new MipsImmediate(0)
                ));
            }
            for (int i = MipsPhysicalRegister.a.length; i < call.getOperands().size(); i++) {
                list.add(new MipsILoadStore(MipsILoadStore.X.sw,
                    eliminateImmediate(list, call.getOperand(i)),
                    MipsPhysicalRegister.sp, new MipsImmediate(argumentOffset(i))
                ));
            }
            list.add(new MipsIJump(MipsIJump.X.jal, target));
            if (call.getType() != Ty.Void) {
                MipsVirtualRegister register = valueMap.get(call);
                list.add(new MipsIBinary(MipsIBinary.X.addiu, register, MipsPhysicalRegister.v0, new MipsImmediate(0)));
            }
            return list;
        }
        if (instruction instanceof IReturn ret) {
            MipsBlock.addEdge(currentBlock, exitBlock);

            if (ret.getOperands().isEmpty()) {
                return List.of(new MipsIJump(MipsIJump.X.j, exitBlock));
            }
            if (ret.getOperand(0) instanceof ConstInteger integer) {
                return List.of(
                    new MipsIBinary(MipsIBinary.X.addiu, MipsPhysicalRegister.v0, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())),
                    new MipsIJump(MipsIJump.X.j, exitBlock)
                );
            }
            return List.of(
                new MipsIBinary(MipsIBinary.X.addiu, MipsPhysicalRegister.v0, valueMap.get(ret.getOperand(0)), new MipsImmediate(0)),
                new MipsIJump(MipsIJump.X.j, exitBlock)
            );
        }
        if (instruction instanceof ILoad load) {
            MipsVirtualRegister register = valueMap.get(load);
            MipsILoadStore.X opcode;
            if (load.getType().sizeof() == 1) {
                opcode = MipsILoadStore.X.lbu;
            } else if (load.getType().sizeof() == 2) {
                opcode = MipsILoadStore.X.lhu;
            } else if (load.getType().sizeof() == 4) {
                opcode = MipsILoadStore.X.lw;
            } else {
                throw new RuntimeException("Unsupported load size");
            }
            if (load.getOperand(0).getName().startsWith("@")) {
                return List.of(new MipsILoadStore(
                    opcode, register, MipsPhysicalRegister.zero,
                    new MipsImmediate(globalValueToMipsTag(load.getOperand(0)))
                ));
            }
            return List.of(new MipsILoadStore(opcode, register, valueMap.get(load.getOperand(0)), new MipsImmediate(0)));
        }
        if (instruction instanceof IStore store) {
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
            if (move.getOperand(0) instanceof ConstInteger integer) {
                MipsVirtualRegister register = valueMap.get(move);
                return List.of(new MipsIBinary(MipsIBinary.X.addiu, register, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())));
            }
            valueMap.put(move, valueMap.get(move.getOperand(0)));
            return List.of();
        }
        if (instruction instanceof IConvert convert) {
            MipsVirtualRegister register = valueMap.get(convert);
            if (convert.isTruncating()) {
                if (convert.getOperand(0) instanceof ConstInteger integer) {
                    return List.of(new MipsIBinary(MipsIBinary.X.addiu, register, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue() % (1 << convert.getType().sizeof() * 8))));
                } else {
                    return List.of(new MipsIBinary(MipsIBinary.X.andi, register, valueMap.get(convert.getOperand(0)), new MipsImmediate((1 << convert.getType().sizeof() * 8) - 1)));
                }
            } else {
                if (convert.getOperand(0) instanceof ConstInteger integer) {
                    return List.of(new MipsIBinary(MipsIBinary.X.addiu, register, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())));
                } else {
                    return List.of(new MipsIBinary(MipsIBinary.X.addiu, register, valueMap.get(convert.getOperand(0)), new MipsImmediate(0)));
                }
            }
        }
        if (instruction instanceof IPhi phi) {
            MipsVirtualRegister register = valueMap.get(phi);
            MipsIPhi mipsPhi = new MipsIPhi(register);
            for (Pair<BasicBlock, Value> pair : phi.getIncoming()) {
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
            List<MipsInstruction> res = new LinkedList<>();
            MipsVirtualRegister register = valueMap.get(gep);
            Value pointer = gep.getOperand(0);
            MipsOperand pointerOperand;
            if (pointer.getName().startsWith("@")) {
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
                base = ((TyPtr) pointer.getType()).getBase();
            } else {
                base = ((TyArray) ((TyPtr) pointer.getType()).getBase()).getBase();
            }
            if (offset instanceof ConstInteger integer) {
                res.add(new MipsIBinary(
                    MipsIBinary.X.addiu, register, pointerOperand,
                    new MipsImmediate(integer.getValue() * base.sizeof()))
                );
            } else {
                MipsVirtualRegister temp = new MipsVirtualRegister();
                res.addAll(List.of(
                    new MipsIBitShift(
                        MipsIBitShift.X.sll, temp, valueMap.get(offset),
                        // ceil(log2(x)) = 32 - numberOfLeadingZeros(x - 1)
                        new MipsImmediate(32 - Integer.numberOfLeadingZeros(base.sizeof() - 1))
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

    private List<MipsInstruction> buildShiftHelper(User user, MipsIBitShift.X opcode, MipsIBitShift.X opcodeI) {
        List<MipsInstruction> list = new LinkedList<>();
        Value lhs = user.getOperand(0);
        Value rhs = user.getOperand(1);
        MipsOperand lhsOperand;
        if (lhs instanceof ConstInteger integer) {
            lhsOperand = new MipsVirtualRegister();
            list.add(new MipsIBinary(MipsIBinary.X.addiu, lhsOperand, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())));
        } else {
            lhsOperand = valueMap.get(lhs);
        }
        MipsVirtualRegister result = valueMap.get(user);
        if (rhs instanceof ConstInteger integer) {
            list.add(new MipsIBitShift(opcodeI, result, lhsOperand, new MipsImmediate(integer.getValue())));
        } else {
            list.add(new MipsIBitShift(opcode, result, lhsOperand, valueMap.get(rhs)));
        }
        return list;
    }

    private MipsOperand eliminateImmediate(List<MipsInstruction> instructions, Value value) {
        if (value instanceof ConstInteger integer) {
            MipsVirtualRegister register = new MipsVirtualRegister();
            valueMap.put(value, register);
            instructions.add(new MipsIBinary(MipsIBinary.X.addiu, register, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())));
            return register;
        }
        return valueMap.get(value);
    }
}
