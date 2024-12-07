package top.swkfk.compiler.arch.mips.process;

import top.swkfk.compiler.arch.mips.instruction.MipsIBinary;
import top.swkfk.compiler.arch.mips.instruction.MipsISyscall;
import top.swkfk.compiler.arch.mips.instruction.MipsInstruction;
import top.swkfk.compiler.arch.mips.operand.MipsImmediate;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.operand.MipsVirtualRegister;
import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.llvm.value.constants.ConstInteger;
import top.swkfk.compiler.utils.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Build the template instructions for MIPS. In java doc for the methods, the following symbols are used:
 * <li>{@code $$res} represents the result (virtual) register of the binary operation</li>
 * <li>{@code $$xxx} represents the (virtual) register operand</li>
 * <li>{@code ##xxx} represents the immediate operand</li>
 * <li>{@code $yy} represents the physical register operand</li>
 * In the future, most of them will be replaced by the pseudo instructions.
 */
final public class MipsInstructionHub {

    /**
     * <li>{@code sub $$res, $$lhs, $$rhs}</li>
     * <li>{@code sub $$res, $$lhs, ##rhs}</li>
     * <li>{@code sub $$res, ##lhs, $$rhs}</li>
     */
    static List<MipsInstruction> sub(User binary, Map<Value, MipsVirtualRegister> valueMap) {
        Value lhs = binary.getOperand(0);
        Value rhs = binary.getOperand(1);
        if (lhs instanceof ConstInteger integer) {
            // c - x => y <- c; y - x
            MipsVirtualRegister register = valueMap.get(binary);
            return List.of(
                // X[temporary] <- lhs
                new MipsIBinary(
                    MipsIBinary.X.addiu, MipsPhysicalRegister.at, MipsPhysicalRegister.zero,
                    new MipsImmediate(integer.getValue())
                ),
                // X[register] <- X[temporary] - X[rhs]
                new MipsIBinary(
                    MipsIBinary.X.subu, register, MipsPhysicalRegister.at, valueMap.get(rhs)
                )
            );
        } else if (rhs instanceof ConstInteger integer) {
            // x - c => x + (-c)
            MipsVirtualRegister register = valueMap.get(binary);
            return List.of(new MipsIBinary(
                MipsIBinary.X.addiu, register, valueMap.get(lhs),
                new MipsImmediate(-integer.getValue())
            ));
        } else {
            // x - y
            MipsVirtualRegister register = valueMap.get(binary);
            return List.of(new MipsIBinary(MipsIBinary.X.subu, register, valueMap.get(lhs), valueMap.get(rhs)));
        }
    }



    /**
     * <li>{@code seq $$res, $$lhs, $$rhs}</li>
     * <li>{@code seq $$res, $$lhs, ##rhs}</li>
     * <li>{@code seq $$res, ##lhs, $$rhs}</li>
     * @deprecated
     */
    @SuppressWarnings("Duplicates")
    static List<MipsInstruction> seq(User binary, Map<Value, MipsVirtualRegister> valueMap) {
        List<MipsInstruction> result = new LinkedList<>();
        MipsOperand lhs = convertImmediate(binary.getOperand(0), valueMap, result);
        MipsOperand rhs = convertImmediate(binary.getOperand(1), valueMap, result);
        MipsVirtualRegister register = new MipsVirtualRegister();

        // X[result] <- X[lhs] - X[rhs]
        result.add(new MipsIBinary(MipsIBinary.X.subu, register, lhs, rhs));
        // X[$at] <- X[result] | 0000_0001h
        result.add(new MipsIBinary(MipsIBinary.X.ori, MipsPhysicalRegister.at, register, new MipsImmediate(1)));
        // X[result] <- X[result] <u X[$at]
        result.add(new MipsIBinary(MipsIBinary.X.sltu, register, register, MipsPhysicalRegister.at));

        valueMap.put(binary, register);
        return result;
    }

    /**
     * <li>{@code sne $$res, $$lhs, $$rhs}</li>
     * <li>{@code sne $$res, $$lhs, ##rhs}</li>
     * <li>{@code sne $$res, ##lhs, $$rhs}</li>
     * @deprecated
     */
    @SuppressWarnings("Duplicates")
    static List<MipsInstruction> sne(User binary, Map<Value, MipsVirtualRegister> valueMap) {
        List<MipsInstruction> result = new LinkedList<>();
        MipsOperand lhs = convertImmediate(binary.getOperand(0), valueMap, result);
        MipsOperand rhs = convertImmediate(binary.getOperand(1), valueMap, result);
        MipsVirtualRegister register = new MipsVirtualRegister();

        // X[result] <- X[lhs] - X[rhs]
        result.add(new MipsIBinary(MipsIBinary.X.subu, register, lhs, rhs));
        // X[result] <- 0h <u X[result]
        result.add(new MipsIBinary(MipsIBinary.X.sltu, register, MipsPhysicalRegister.zero, register));

        valueMap.put(binary, register);
        return result;
    }

    /**
     * Inner builder for <code>slt</code> and <code>sgt</code>
     */
    private static List<MipsInstruction> slt(Map<Value, MipsVirtualRegister> valueMap, MipsOperand res, Value lhs, Value rhs) {
        if (rhs instanceof ConstInteger integer) {
            return List.of(new MipsIBinary(
                MipsIBinary.X.slti, res, valueMap.get(lhs), new MipsImmediate(integer.getValue())
            ));
        }

        var pair = convertImmediate(lhs, valueMap);
        MipsOperand lhsOperand = pair.first();
        MipsOperand rhsOperand = valueMap.get(rhs);

        return new LinkedList<>() {{
            addAll(pair.second());
            add(new MipsIBinary(MipsIBinary.X.slt, res, lhsOperand, rhsOperand));
        }};
    }

    /**
     * <li>{@code slt $$res, $$lhs, $$rhs}</li>
     * <li>{@code slt $$res, $$lhs, ##rhs}</li>
     * <li>{@code slt $$res, ##lhs, $$rhs}</li>
     * @deprecated
     */
    static List<MipsInstruction> slt(User binary, Map<Value, MipsVirtualRegister> valueMap) {
        MipsVirtualRegister register = new MipsVirtualRegister();
        valueMap.put(binary, register);
        return slt(valueMap, register, binary.getOperand(0), binary.getOperand(1));
    }

    /**
     * <li>{@code sgt $$res, $$lhs, $$rhs}</li>
     * <li>{@code sgt $$res, $$lhs, ##rhs}</li>
     * <li>{@code sgt $$res, ##lhs, $$rhs}</li>
     * @deprecated
     */
    static List<MipsInstruction> sgt(User binary, Map<Value, MipsVirtualRegister> valueMap) {
        MipsVirtualRegister register = new MipsVirtualRegister();
        valueMap.put(binary, register);
        return slt(valueMap, register, binary.getOperand(1), binary.getOperand(0));
    }

    /**
     * Inner builder for <code>sle</code> and <code>sge</code>
     */
    private static List<MipsInstruction> sle(Map<Value, MipsVirtualRegister> valueMap, MipsOperand res, Value lhs, Value rhs) {
        List<MipsInstruction> result = new LinkedList<>();

        MipsOperand lhsOperand = convertImmediate(lhs, valueMap, result);
        MipsOperand rhsOperand = convertImmediate(rhs, valueMap, result);
        MipsVirtualRegister register = new MipsVirtualRegister();

        // X[result] <- X[lhs] < X[rhs]
        result.add(new MipsIBinary(MipsIBinary.X.slt, register, lhsOperand, rhsOperand));
        // X[$at] <- X[result] | 0000_0001h
        result.add(new MipsIBinary(MipsIBinary.X.ori, MipsPhysicalRegister.at, register, new MipsImmediate(1)));
        // X[result] <- X[$at] -u X[result]
        result.add(new MipsIBinary(MipsIBinary.X.subu, res, MipsPhysicalRegister.at, register));

        valueMap.put(lhs, register);
        return result;
    }

    /**
     * <li>{@code sle $$res, $$lhs, $$rhs}</li>
     * <li>{@code sle $$res, $$lhs, ##rhs}</li>
     * <li>{@code sle $$res, ##lhs, $$rhs}</li>
     * @deprecated
     */
    static List<MipsInstruction> sle(User binary, Map<Value, MipsVirtualRegister> valueMap) {
        MipsVirtualRegister register = new MipsVirtualRegister();
        valueMap.put(binary, register);
        return sle(valueMap, register, binary.getOperand(0), binary.getOperand(1));
    }

    /**
     * <li>{@code sge $$res, $$lhs, $$rhs}</li>
     * <li>{@code sge $$res, $$lhs, ##rhs}</li>
     * <li>{@code sge $$res, ##lhs, $$rhs}</li>
     * @deprecated
     */
    static List<MipsInstruction> sge(User binary, Map<Value, MipsVirtualRegister> valueMap) {
        MipsVirtualRegister register = new MipsVirtualRegister();
        valueMap.put(binary, register);
        return sle(valueMap, register, binary.getOperand(1), binary.getOperand(0));
    }

    /**
     * Convert the immediate value to the register (<b>use $at</b>) and get the register operand if not.
     * @param value the value to be converted
     * @param valueMap the value map
     * @return the pair of the register operand and the instructions added to convert the immediate value
     */
    private static Pair<MipsOperand, List<MipsInstruction>> convertImmediate(Value value, Map<Value, MipsVirtualRegister> valueMap) {
        if (value instanceof ConstInteger integer) {
            MipsPhysicalRegister register = MipsPhysicalRegister.at;
            return new Pair<>(register, List.of(new MipsIBinary(
                MipsIBinary.X.addiu, register, MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())
            )));
        }
        return new Pair<>(valueMap.get(value), List.of());
    }

    /**
     * Convert the immediate value to the register and get the register operand if not. Add the instructions to the receiver.
     * @see #convertImmediate(Value, Map)
     */
    private static MipsOperand convertImmediate(Value value, Map<Value, MipsVirtualRegister> valueMap, List<MipsInstruction> receiver) {
        var pair = convertImmediate(value, valueMap);
        receiver.addAll(pair.second());
        return pair.first();
    }

    public static List<MipsInstruction> buildSyscallRead(int syscall, MipsOperand receiver) {
        return List.of(
            new MipsIBinary(MipsIBinary.X.addiu, MipsPhysicalRegister.v0, MipsPhysicalRegister.zero, new MipsImmediate(syscall)),
            new MipsISyscall(),
            new MipsIBinary(MipsIBinary.X.addiu, receiver, MipsPhysicalRegister.v0, new MipsImmediate(0))
        );
    }

    public static List<MipsInstruction> buildSyscallWrite(int syscall, Value value, Map<Value, MipsVirtualRegister> valueMap) {
        MipsInstruction loadValue;
        if (value instanceof ConstInteger integer) {
            loadValue = new MipsIBinary(
                MipsIBinary.X.addiu, MipsPhysicalRegister.a[0], MipsPhysicalRegister.zero, new MipsImmediate(integer.getValue())
            );
        } else {
            loadValue = new MipsIBinary(
                MipsIBinary.X.addiu, MipsPhysicalRegister.a[0], valueMap.get(value), new MipsImmediate(0)
            );
        }
        return List.of(
            loadValue,
            new MipsIBinary(MipsIBinary.X.addiu, MipsPhysicalRegister.v0, MipsPhysicalRegister.zero, new MipsImmediate(syscall)),
            new MipsISyscall()
        );
    }
}
