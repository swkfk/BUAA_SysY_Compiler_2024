package top.swkfk.compiler.arch.mips.process;

import top.swkfk.compiler.arch.mips.MipsBlock;
import top.swkfk.compiler.arch.mips.MipsFunction;
import top.swkfk.compiler.arch.mips.instruction.MipsIBinary;
import top.swkfk.compiler.arch.mips.instruction.MipsIBitShift;
import top.swkfk.compiler.arch.mips.instruction.MipsIBrEqu;
import top.swkfk.compiler.arch.mips.instruction.MipsIHiLo;
import top.swkfk.compiler.arch.mips.instruction.MipsIJump;
import top.swkfk.compiler.arch.mips.instruction.MipsILoadAddress;
import top.swkfk.compiler.arch.mips.instruction.MipsILoadStore;
import top.swkfk.compiler.arch.mips.instruction.MipsIMultDiv;
import top.swkfk.compiler.arch.mips.instruction.MipsIPhi;
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
/// |            The 5th Argument            |
/// +----------------------------------------+  <-- Caller $fp - 4
/// |            The 6th Argument            |
/// +----------------------------------------+  <-- Caller $fp - 8
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
    private int stackSize = 0;

    public MipsGenerator(DualLinkedList<BasicBlock> blocks, Map<Function, MipsFunction> functionMap, MipsBlock entry, MipsBlock exit) {
        blocks.forEach(node -> blockMap.put(node.getData(), new MipsBlock(node.getData())));
        this.functionMap = functionMap;
        this.exitBlock = exit;
        MipsBlock.addEdge(entry, blockLLVM2Mips(blocks.getHead().getData()));
        // This will be added in the Return instruction
        // MipsBlock.addEdge(blockLLVM2Mips(blocks.getTail().getData()), exit);
    }

    public MipsBlock blockLLVM2Mips(BasicBlock block) {
        return blockMap.get(block);
    }

    public List<MipsInstruction> addParameter(Value value, int index) {
        MipsVirtualRegister register = new MipsVirtualRegister();
        valueMap.put(value, register);
        if (index >= MipsPhysicalRegister.a.length) {
            enlargeStack(4);
            return List.of(
                new MipsILoadStore(MipsILoadStore.X.lw, register, MipsPhysicalRegister.fp, new MipsImmediate((1 + index - MipsPhysicalRegister.a.length) * -4))
            );
        } else {
            return List.of(
                new MipsIBinary(MipsIBinary.X.addiu, register, MipsPhysicalRegister.a[index], new MipsImmediate(0))
            );
        }
    }

    public List<MipsInstruction> run(MipsBlock currentBlock, User instruction) {
        if (instruction instanceof IAllocate allocate) {
            int offset = enlargeStack(((TyPtr) allocate.getType()).getBase().sizeof());
            MipsVirtualRegister register = new MipsVirtualRegister();
            valueMap.put(allocate, register);
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
                MipsVirtualRegister register = new MipsVirtualRegister();
                valueMap.put(instruction, register);
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
                MipsVirtualRegister cond = valueMap.get(branch.getOperand(0));
                Pair<BasicBlock, BasicBlock> target = branch.getConditionalTarget();
                MipsBlock.addEdge(currentBlock, blockLLVM2Mips(target.first()));
                MipsBlock.addEdge(currentBlock, blockLLVM2Mips(target.second()));

                return List.of(
                    new MipsIBrEqu(MipsIBrEqu.X.bne, cond, MipsPhysicalRegister.zero, blockLLVM2Mips(target.first())),
                    new MipsIJump(MipsIJump.X.j, blockLLVM2Mips(target.second()))
                );
            } else {
                MipsBlock.addEdge(currentBlock, blockLLVM2Mips(branch.getTarget()));
                return List.of(new MipsIJump(MipsIJump.X.j, blockLLVM2Mips(branch.getTarget())));
            }
        }
        if (instruction instanceof ICall call) {
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
                    MipsPhysicalRegister.sp, new MipsImmediate((1 + i - MipsPhysicalRegister.a.length) * -4)
                ));
            }
            list.add(new MipsIJump(MipsIJump.X.jal, target));
            if (call.getType() != Ty.Void) {
                MipsVirtualRegister register = new MipsVirtualRegister();
                list.add(new MipsIBinary(MipsIBinary.X.addiu, register, MipsPhysicalRegister.v0, new MipsImmediate(0)));
                valueMap.put(call, register);
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
            MipsVirtualRegister register = new MipsVirtualRegister();
            valueMap.put(load, register);
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
                MipsVirtualRegister register = new MipsVirtualRegister();
                valueMap.put(move, register);
                return List.of(new MipsIBinary(MipsIBinary.X.addiu, register, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())));
            }
            valueMap.put(move, valueMap.get(move.getOperand(0)));
            return List.of();
        }
        if (instruction instanceof IConvert convert) {
            MipsVirtualRegister register = new MipsVirtualRegister();
            valueMap.put(convert, register);
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
            MipsVirtualRegister register = new MipsVirtualRegister();
            valueMap.put(phi, register);
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
            MipsVirtualRegister register = new MipsVirtualRegister();
            valueMap.put(gep, register);
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
                res.addAll(List.of(
                    new MipsIBitShift(
                        MipsIBitShift.X.sll, register, valueMap.get(offset),
                        // ceil(log2(x)) = 32 - numberOfLeadingZeros(x - 1)
                        new MipsImmediate(32 - Integer.numberOfLeadingZeros(base.sizeof() - 1))
                    ),
                    new MipsIBinary(MipsIBinary.X.addu, register, pointerOperand, valueMap.get(offset))
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
        MipsVirtualRegister register = new MipsVirtualRegister();
        valueMap.put(binary, register);
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
            MipsVirtualRegister register = new MipsVirtualRegister();
            valueMap.put(binary, register);
            return List.of(new MipsIBinary(opcodeI, register, valueMap.get(rhs), new MipsImmediate(integer.getValue())));
        }
        if (rhs instanceof ConstInteger integer) {
            MipsVirtualRegister register = new MipsVirtualRegister();
            valueMap.put(binary, register);
            return List.of(new MipsIBinary(opcodeI, register, valueMap.get(lhs), new MipsImmediate(integer.getValue())));
        }
        MipsVirtualRegister register = new MipsVirtualRegister();
        valueMap.put(binary, register);
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
        MipsVirtualRegister resultRegister = new MipsVirtualRegister();
        if (lhs instanceof ConstInteger integer) {
            list.add(new MipsIBinary(MipsIBinary.X.addiu, MipsPhysicalRegister.at, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())));
            list.add(new MipsIMultDiv(opcode, MipsPhysicalRegister.at, valueMap.get(rhs)));
        } else if (rhs instanceof ConstInteger integer) {
            list.add(new MipsIBinary(MipsIBinary.X.addiu, MipsPhysicalRegister.at, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())));
            list.add(new MipsIMultDiv(opcode, valueMap.get(lhs), MipsPhysicalRegister.at));
        } else {
            list.add(new MipsIMultDiv(opcode, valueMap.get(lhs), valueMap.get(rhs)));
        }
        valueMap.put(binary, resultRegister);
        list.add(new MipsIHiLo(mf, resultRegister));
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
