package top.swkfk.compiler.arch.mips.process;

import top.swkfk.compiler.arch.mips.MipsBlock;
import top.swkfk.compiler.arch.mips.MipsFunction;
import top.swkfk.compiler.arch.mips.instruction.MipsIBinary;
import top.swkfk.compiler.arch.mips.instruction.MipsIBrEqu;
import top.swkfk.compiler.arch.mips.instruction.MipsIHiLo;
import top.swkfk.compiler.arch.mips.instruction.MipsIJump;
import top.swkfk.compiler.arch.mips.instruction.MipsILoadStore;
import top.swkfk.compiler.arch.mips.instruction.MipsIMultDiv;
import top.swkfk.compiler.arch.mips.instruction.MipsIUnimp;
import top.swkfk.compiler.arch.mips.instruction.MipsInstruction;
import top.swkfk.compiler.arch.mips.operand.MipsImmediate;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.operand.MipsVirtualRegister;
import top.swkfk.compiler.frontend.symbol.type.Ty;
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
import top.swkfk.compiler.llvm.value.instruction.ILoad;
import top.swkfk.compiler.llvm.value.instruction.IMove;
import top.swkfk.compiler.llvm.value.instruction.IReturn;
import top.swkfk.compiler.llvm.value.instruction.IStore;
import top.swkfk.compiler.utils.DualLinkedList;
import top.swkfk.compiler.utils.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

final public class MipsGenerator {
    private final Map<Value, MipsVirtualRegister> valueMap = new HashMap<>();
    private final Map<BasicBlock, MipsBlock> blockMap = new HashMap<>();
    private final Map<Function, MipsFunction> functionMap;
    private final MipsBlock exitBlock;
    private int stackSize = 0;

    public MipsGenerator(DualLinkedList<BasicBlock> blocks, Map<Function, MipsFunction> functionMap, MipsBlock exit) {
        blocks.forEach(node -> blockMap.put(node.getData(), new MipsBlock(node.getData())));
        this.functionMap = functionMap;
        this.exitBlock = exit;
    }

    public MipsBlock blockLLVM2Mips(BasicBlock block) {
        return blockMap.get(block);
    }

    public List<MipsInstruction> run(User instruction) {
        if (instruction instanceof IAllocate allocate) {
            int offset = enlargeStack(((TyPtr) allocate.getType()).getBase().sizeof());
            MipsVirtualRegister register = new MipsVirtualRegister();
            valueMap.put(allocate, register);
            return List.of(
                new MipsIBinary(MipsIBinary.X.addiu, register, MipsPhysicalRegister.sp, new MipsImmediate(offset))
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
                return List.of(
                    new MipsIBrEqu(MipsIBrEqu.X.bne, cond, MipsPhysicalRegister.zero, blockLLVM2Mips(target.first())),
                    new MipsIJump(MipsIJump.X.j, blockLLVM2Mips(target.second()))
                );
            } else {
                return List.of(new MipsIJump(MipsIJump.X.j, blockLLVM2Mips(branch.getTarget())));
            }
        }
        if (instruction instanceof ICall call) {
            List<MipsInstruction> list = new LinkedList<>();
            /// Convention:
            /// $a0 ~ $a3: The first four arguments
            ///
            /// |                                        |  <-- Caller's Stack Frame
            /// +========================================+  <-- Caller's $sp
            /// |                                        |  <-- The 5th argument
            /// +----------------------------------------+  <-- $sp - 4
            /// |                                        |  <-- The 6th argument
            /// +----------------------------------------+  <-- $sp - 8
            ///
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
            list.add(new MipsIJump(MipsIJump.X.jal, functionMap.get(call.getFunction()).getEntryBlock()));
            if (call.getType() != Ty.Void) {
                MipsVirtualRegister register = new MipsVirtualRegister();
                list.add(new MipsIBinary(MipsIBinary.X.addiu, register, MipsPhysicalRegister.v0, new MipsImmediate(0)));
                valueMap.put(call, register);
            }
            return list;
        }
        if (instruction instanceof IReturn ret) {
            if (ret.getOperands().isEmpty()) {
                return List.of(new MipsIJump(MipsIJump.X.j, exitBlock));
            }
            if (ret.getOperand(0) instanceof ConstInteger integer) {
                return List.of(
                    new MipsIBinary(MipsIBinary.X.addiu, MipsPhysicalRegister.v0, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())),
                    new MipsIJump(MipsIJump.X.j, exitBlock)
                );
            }
            MipsVirtualRegister register = new MipsVirtualRegister();
            return List.of(
                new MipsIBinary(MipsIBinary.X.addiu, register, eliminateImmediate(List.of(), ret.getOperand(0)), new MipsImmediate(0)),
                new MipsIJump(MipsIJump.X.j, exitBlock)
            );
        }
        if (instruction instanceof ILoad load) {
            MipsVirtualRegister register = new MipsVirtualRegister();
            valueMap.put(load, register);
            return List.of(new MipsILoadStore(MipsILoadStore.X.lw, register, valueMap.get(load.getOperand(0)), new MipsImmediate(0)));
        }
        if (instruction instanceof IStore store) {
            List<MipsInstruction> res = new LinkedList<>();
            MipsOperand operand;
            if (store.getOperand(0) instanceof ConstInteger integer) {
                operand = new MipsVirtualRegister();
                res.add(new MipsIBinary(MipsIBinary.X.addiu, operand, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())));
            } else {
                operand = valueMap.get(store.getOperand(0));
            }
            res.add(new MipsILoadStore(MipsILoadStore.X.sw,
                operand, valueMap.get(store.getOperand(1)), new MipsImmediate(0)
            ));
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
        return List.of(new MipsIUnimp());
    }

    private int enlargeStack(int size) {
        int offset = stackSize;
        stackSize += size;
        return offset;
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
