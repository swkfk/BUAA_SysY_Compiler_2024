package top.swkfk.compiler.arch.mips.process;

import top.swkfk.compiler.arch.mips.MipsBlock;
import top.swkfk.compiler.arch.mips.MipsFunction;
import top.swkfk.compiler.arch.mips.instruction.MipsIBinary;
import top.swkfk.compiler.arch.mips.instruction.MipsIBrEqu;
import top.swkfk.compiler.arch.mips.instruction.MipsIBrZero;
import top.swkfk.compiler.arch.mips.instruction.MipsIJump;
import top.swkfk.compiler.arch.mips.instruction.MipsILoadStore;
import top.swkfk.compiler.arch.mips.instruction.MipsInstruction;
import top.swkfk.compiler.arch.mips.operand.MipsImmediate;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.operand.MipsVirtualRegister;
import top.swkfk.compiler.helpers.BlockLivingData;
import top.swkfk.compiler.utils.DualLinkedList;
import top.swkfk.compiler.utils.Pair;
import top.swkfk.compiler.utils.UndirectedGraph;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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

    private boolean haveNotAllocated(MipsVirtualRegister register) {
        return !allocated.containsKey(register) && !spilled.contains(register);
    }

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
                if (instruction instanceof MipsIJump || instruction instanceof MipsIBrZero || instruction instanceof MipsIBrEqu) {
                    blockLevel++;
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

    private MipsVirtualRegister fitLocalAllocate(MipsOperand operand, MipsBlock block) {
        if (operand instanceof MipsVirtualRegister register && localVirtualRegisters.get(block).contains(register)) {
            return register;
        }
        return null;
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
                    MipsVirtualRegister register = fitLocalAllocate(operand, block);
                    if (register == null) {
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
                    MipsVirtualRegister register = fitLocalAllocate(operand, block);
                    if (register == null) {
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
                    MipsVirtualRegister register = fitLocalAllocate(operand, block);
                    if (register == null) {
                        continue;
                    }
                    if (spilled.contains(register) || allocated.containsKey(register)) {
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

    /**
     * Apply the simple, block-level graph coloring algorithm to allocate the global registers.
     * @return this
     */
    public MipsFunctionRegisterAllocate runAllocateGlobalRegisters() {
        HashMap<MipsBlock, BlockLivingData<MipsVirtualRegister>> livings = new HashMap<>();
        // 1. Initialize the living data & use/def sets
        for (DualLinkedList.Node<MipsBlock> bNode : function.getBlocks()) {
            MipsBlock block = bNode.getData();
            livings.put(block, new BlockLivingData<>());
            // Generate the use/def of this block
            HashSet<MipsVirtualRegister> use = new HashSet<>(), def = new HashSet<>();
            for (DualLinkedList.Node<MipsInstruction> iNode : block.getInstructions()) {
                MipsInstruction instruction = iNode.getData();
                use.addAll(
                    Arrays.stream(instruction.getUseVirtualRegisters())
                        .filter(register -> haveNotAllocated(register) && !def.contains(register))
                        .toList()
                );
                def.addAll(
                    Arrays.stream(instruction.getDefVirtualRegisters())
                        .filter(register -> haveNotAllocated(register) && !use.contains(register))
                        .toList()
                );
            }
            livings.get(block).getUse().addAll(use);
            livings.get(block).getDef().addAll(def);
        }
        // 2. Calculate the in/out sets
        boolean changed = true;
        while (changed) {
            changed = false;
            for (DualLinkedList.Node<MipsBlock> bNode : function.getBlocks()) {
                MipsBlock block = bNode.getData();
                BlockLivingData<MipsVirtualRegister> living = livings.get(block);
                Set<MipsVirtualRegister> out = new HashSet<>();
                for (MipsBlock successor : block.getSuccessors()) {
                    BlockLivingData.union(out, livings.get(successor).getIn());
                }
                Set<MipsVirtualRegister> in = new HashSet<>(out);
                BlockLivingData.minus(in, living.getDef());
                BlockLivingData.union(in, living.getUse());
                if (!in.equals(living.getIn()) || !out.equals(living.getOut())) {
                    changed = true;
                    living.getIn().clear();
                    living.getIn().addAll(in);
                    living.getOut().clear();
                    living.getOut().addAll(out);
                }
            }
        }
        // Make up the missive virtual registers
        for (DualLinkedList.Node<MipsBlock> bNode : function.getBlocks()) {
            MipsBlock block = bNode.getData();
            for (DualLinkedList.Node<MipsInstruction> iNode : block.getInstructions()) {
                MipsInstruction instruction = iNode.getData();
                for (MipsVirtualRegister register : instruction.getUseVirtualRegisters()) {
                    if (haveNotAllocated(register)) {
                        livings.get(block).getOut().add(register);
                    }
                }
                for (MipsVirtualRegister register : instruction.getDefVirtualRegisters()) {
                    if (haveNotAllocated(register)) {
                        livings.get(block).getOut().add(register);
                    }
                }
            }
        }
        // 3. Calculate the interference graph
        UndirectedGraph<MipsVirtualRegister> interferenceGraph = new UndirectedGraph<>();
        for (DualLinkedList.Node<MipsBlock> bNode : function.getBlocks()) {
            MipsBlock block = bNode.getData();
            BlockLivingData<MipsVirtualRegister> living = livings.get(block);
            for (MipsVirtualRegister register : living.getIn()) {
                interferenceGraph.addVertex(register);
            }
            for (MipsVirtualRegister register : living.getOut()) {
                interferenceGraph.addVertex(register);
            }
            for (MipsVirtualRegister register : living.getOut()) {
                for (MipsVirtualRegister other : living.getOut()) {
                    if (register != other) {
                        interferenceGraph.addEdge(register, other);
                    }
                }
                for (MipsVirtualRegister other : living.getIn()) {
                    if (register != other) {
                        interferenceGraph.addEdge(register, other);
                    }
                }
            }
        }
        // 4. Prepare the data structure for the graph coloring
        int K = Config.globalRegisters.size();
        List<MipsVirtualRegister> removed = new LinkedList<>();
        UndirectedGraph<MipsVirtualRegister> originGraph = interferenceGraph.copy();
        HashMap<MipsVirtualRegister, Integer> color = new HashMap<>();
        // 5. Simplified graph coloring algorithm
        while (!interferenceGraph.getVertices().isEmpty()) {
            MipsVirtualRegister register = null;
            for (MipsVirtualRegister vertex : interferenceGraph.getVertices()) {
                if (interferenceGraph.getEdges(vertex).size() < K) {
                    register = vertex;
                    break;
                }
            }
            if (register == null) {
                // TODO: Spill the register with the highest degree
                register = interferenceGraph.getVertices().iterator().next();
                spilled.add(register);
            } else {
                removed.add(register);
            }
            interferenceGraph.removeVertex(register);
        }
        // 6. Allocate the global registers
        for (MipsVirtualRegister register : removed) {
            Set<Integer> usedColors = new HashSet<>();
            for (MipsVirtualRegister other : originGraph.getEdges(register)) {
                if (color.containsKey(other)) {
                    usedColors.add(color.get(other));
                }
            }
            for (int i = 0; i < K; i++) {
                if (!usedColors.contains(i)) {
                    color.put(register, i);
                    allocated.put(register, Config.globalRegisters.get(i));
                    allocatedGlobal.add(Config.globalRegisters.get(i));
                    break;
                }
            }
        }
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
            function.getEntryBlock().addInstructionAfter(
                new MipsILoadStore(MipsILoadStore.X.sw, register, MipsPhysicalRegister.sp, new MipsImmediate(currentOffset)),
                instruction -> instruction instanceof MipsIBinary && instruction.getOperands()[0] == MipsPhysicalRegister.fp
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
