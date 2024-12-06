package top.swkfk.compiler.arch.mips.process;

import top.swkfk.compiler.arch.mips.MipsBlock;
import top.swkfk.compiler.arch.mips.MipsFunction;
import top.swkfk.compiler.arch.mips.instruction.MipsIBrEqu;
import top.swkfk.compiler.arch.mips.instruction.MipsIBrZero;
import top.swkfk.compiler.arch.mips.instruction.MipsIJump;
import top.swkfk.compiler.arch.mips.instruction.MipsILoadStore;
import top.swkfk.compiler.arch.mips.instruction.MipsInstruction;
import top.swkfk.compiler.arch.mips.operand.MipsImmediate;
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
        static final List<MipsPhysicalRegister> temporaryRegisters = Arrays.asList(MipsPhysicalRegister.k0, MipsPhysicalRegister.k1);
    }

    private final MipsFunction function;
    private final HashMap<MipsBlock, Set<MipsVirtualRegister>> localVirtualRegisters;
    private final Set<MipsVirtualRegister> globalVirtualRegisters;

    private final Map<MipsVirtualRegister, MipsPhysicalRegister> allocated = new HashMap<>();
    private final Set<MipsVirtualRegister> spilled = new HashSet<>();
    private final Set<MipsPhysicalRegister> allocatedGlobal = new HashSet<>();


    public MipsFunctionRegisterAllocate(MipsFunction function) {
        this.function = function;
        this.localVirtualRegisters = new HashMap<>();
        this.globalVirtualRegisters = new HashSet<>();
        this.allocatedGlobal.add(MipsPhysicalRegister.ra);
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

        return this;
    }

    /**
     * Apply the simplified linear scan algorithm to allocate the temporary registers.
     * @return this
     */
    public MipsFunctionRegisterAllocate runAllocateTemporaryRegisters() {
        for (DualLinkedList.Node<MipsBlock> bNode : function.getBlocks()) {
            MipsBlock block = bNode.getData();
            if (localVirtualRegisters.getOrDefault(block, Set.of()).isEmpty()) {
                continue;
            }

            HashMap<MipsVirtualRegister, MipsInstruction> lastLivingMap = new HashMap<>();
            for (DualLinkedList.Node<MipsInstruction> iNode : block.getInstructions()) {
                MipsInstruction instruction = iNode.getData();
                for (MipsOperand operand : instruction.getOperands()) {
                    if (!(operand instanceof MipsVirtualRegister register)) {
                        continue;
                    }
                    lastLivingMap.put(register, instruction);
                }
            }

            HashMap<MipsPhysicalRegister, MipsInstruction> currentAllocateMap = new HashMap<>();
            for (MipsPhysicalRegister register : Config.localRegisters) {
                currentAllocateMap.put(register, null);
            }
            LoopInstruction: for (DualLinkedList.Node<MipsInstruction> iNode : block.getInstructions()) {
                MipsInstruction instruction = iNode.getData();
                for (MipsOperand operand : instruction.getOperands()) {
                    if (!(operand instanceof MipsVirtualRegister register)) {
                        continue;
                    }
                    if (allocated.containsKey(register)) {
                        if (currentAllocateMap.get(allocated.get(register)) == instruction) {
                            // Release the register
                            currentAllocateMap.put(allocated.get(register), null);
                        }
                    }
                }
                for (MipsOperand operand : instruction.getOperands()) {
                    if (!(operand instanceof MipsVirtualRegister register)) {
                        continue;
                    }
                    if (spilled.contains(operand) || allocated.containsKey(operand)) {
                        continue;
                    }
                    // Allocate the register
                    for (MipsPhysicalRegister physicalRegister : Config.localRegisters) {
                        if (currentAllocateMap.get(physicalRegister) == null) {
                            if (lastLivingMap.get(register) != instruction) {
                                currentAllocateMap.put(physicalRegister, lastLivingMap.get(register));
                            }
                            allocated.put(register, physicalRegister);
                            continue LoopInstruction;
                        }
                    }
                    // Cannot allocate the register
                    spilled.add(register);
                }
            }
        }
        return this;
    }

    public MipsFunctionRegisterAllocate runAllocateGlobalRegisters() {
        return this;
    }

    public void refill() {
        // 1. Fill the allocated registers
        for (DualLinkedList.Node<MipsBlock> bNode : function.getBlocks()) {
            MipsBlock block = bNode.getData();
            for (DualLinkedList.Node<MipsInstruction> iNode : block.getInstructions()) {
                MipsInstruction instruction = iNode.getData();
                instruction.fillPhysicalRegister(allocated);
            }
        }

        int currentOffset = 0;
        // 2. Store/Recover the global registers used
        function.enlargeStackSize(4 * allocatedGlobal.size());
        for (MipsPhysicalRegister register : allocatedGlobal) {
            function.getEntryBlock().addInstruction(
                new MipsILoadStore(MipsILoadStore.X.sw, register, MipsPhysicalRegister.sp, new MipsImmediate(currentOffset))
            );
            function.getExitBlock().addInstructionFirst(
                new MipsILoadStore(MipsILoadStore.X.lw, register, MipsPhysicalRegister.sp, new MipsImmediate(currentOffset))
            );
            currentOffset += 4;
        }

        // 3. Fill the spilled registers
        HashMap<MipsVirtualRegister, Integer> spilledOffset = new HashMap<>();
        for (MipsVirtualRegister register : spilled) {
            function.enlargeStackSize(4);
            spilledOffset.put(register, currentOffset);
            currentOffset += 4;
        }
        int temporaryRegisterIndex = 0;
        for (DualLinkedList.Node<MipsBlock> bNode : function.getBlocks()) {
            MipsBlock block = bNode.getData();
            for (DualLinkedList.Node<MipsInstruction> iNode : block.getInstructions()) {
                MipsInstruction instruction = iNode.getData();
                for (MipsVirtualRegister useRegister : instruction.getUseVirtualRegisters()) {
                    if (!spilled.contains(useRegister)) {
                        continue;
                    }
                    MipsPhysicalRegister temp = Config.temporaryRegisters.get(temporaryRegisterIndex ^= 1);
                    new DualLinkedList.Node<MipsInstruction>(new MipsILoadStore(
                        MipsILoadStore.X.lw, temp, MipsPhysicalRegister.sp, new MipsImmediate(spilledOffset.get(useRegister))
                    )).insertBefore(iNode);
                    instruction.fillPhysicalRegister(Map.of(useRegister, temp));
                }
                for (MipsVirtualRegister defRegister : instruction.getDefVirtualRegisters()) {
                    if (!spilled.contains(defRegister)) {
                        continue;
                    }
                    MipsPhysicalRegister temp = Config.temporaryRegisters.get(temporaryRegisterIndex ^= 1);
                    instruction.fillPhysicalRegister(Map.of(defRegister, temp));
                    new DualLinkedList.Node<MipsInstruction>(new MipsILoadStore(
                        MipsILoadStore.X.sw, temp, MipsPhysicalRegister.sp, new MipsImmediate(spilledOffset.get(defRegister))
                    )).insertAfter(iNode);
                }
            }
        }
    }
}
