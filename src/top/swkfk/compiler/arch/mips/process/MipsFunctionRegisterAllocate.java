package top.swkfk.compiler.arch.mips.process;

import top.swkfk.compiler.arch.mips.MipsBlock;
import top.swkfk.compiler.arch.mips.MipsFunction;
import top.swkfk.compiler.arch.mips.instruction.MipsIBrEqu;
import top.swkfk.compiler.arch.mips.instruction.MipsIBrZero;
import top.swkfk.compiler.arch.mips.instruction.MipsIJump;
import top.swkfk.compiler.arch.mips.instruction.MipsInstruction;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.operand.MipsVirtualRegister;
import top.swkfk.compiler.utils.DualLinkedList;
import top.swkfk.compiler.utils.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final public class MipsFunctionRegisterAllocate {
    static class Config {
        static final List<MipsPhysicalRegister> localRegisters = Arrays.asList(MipsPhysicalRegister.t);
        static final List<MipsPhysicalRegister> globalRegisters = Arrays.asList(MipsPhysicalRegister.s);
    }

    private final MipsFunction function;
    private final HashMap<MipsBlock, Set<MipsVirtualRegister>> localVirtualRegisters;
    private final Set<MipsVirtualRegister> globalVirtualRegisters;

    public MipsFunctionRegisterAllocate(MipsFunction function) {
        this.function = function;
        this.localVirtualRegisters = new HashMap<>();
        this.globalVirtualRegisters = new HashSet<>();
    }

    public MipsFunctionRegisterAllocate runCheckAllocateStrategy() {
        /// Record the appearance of the virtual register. The value is the block and the 'block level'
        HashMap<MipsVirtualRegister, Pair<MipsBlock, Integer>> records = new HashMap<>();
        for (DualLinkedList.Node<MipsBlock> bNode : function.getBlocks()) {
            MipsBlock block = bNode.getData();
            int blockLevel = 0;
            for (DualLinkedList.Node<MipsInstruction> iNode : block.getInstructions()) {
                MipsInstruction instruction = iNode.getData();
                if (instruction instanceof MipsIJump || instruction instanceof MipsIBrZero || instruction instanceof MipsIBrEqu) {
                    blockLevel++;
                }
                for (MipsOperand operand : instruction.getOperands()) {
                    if (!(operand instanceof MipsVirtualRegister register)) {
                        continue;
                    }
                    if (!records.containsKey(register)) {
                        records.put(register, new Pair<>(block, blockLevel));
                    } else {
                        Pair<MipsBlock, Integer> record = records.get(register);
                        if (record == null || !record.equals(new Pair<>(block, blockLevel))) {
                            globalVirtualRegisters.add(register);
                            records.put(register, null);
                        }
                    }
                }
            }
        }

        /// Those who are just the local registers must be defined in the same block and before every use.
        /// Otherwise, when entering the block the first time, the register will be undefined.

        for (var entry : records.entrySet()) {
            if (entry.getValue() != null) {
                localVirtualRegisters.computeIfAbsent(entry.getValue().first(), k -> new HashSet<>()).add(entry.getKey());
            }
        }

        System.err.println("Checked `" + function + "`:");
        System.err.println("  Local : " + localVirtualRegisters);
        System.err.println("  Global: " + globalVirtualRegisters);
        return this;
    }

    public MipsFunctionRegisterAllocate runAllocateTemporaryRegisters() {
        return this;
    }

    public MipsFunctionRegisterAllocate runAllocateGlobalRegisters() {
        return this;
    }

    /**
     * Check whether every register is allocated.
     */
    public void finalCheck() {
    }
}
