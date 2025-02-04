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
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

final public class MipsFunctionRegisterAllocate {
    static class Config {
        // 可用局部寄存器
        static final List<MipsPhysicalRegister> localRegisters = List.of(
            MipsPhysicalRegister.t[0], MipsPhysicalRegister.t[1], MipsPhysicalRegister.t[2], MipsPhysicalRegister.t[3],
            MipsPhysicalRegister.t[4], MipsPhysicalRegister.t[5], MipsPhysicalRegister.t[6], MipsPhysicalRegister.t[7]
        );
        // 可用全局寄存器，比通常的约定多了 $v1, $t8, $t9, $gp
        static final List<MipsPhysicalRegister> globalRegisters = List.of(
            MipsPhysicalRegister.s[0], MipsPhysicalRegister.s[1], MipsPhysicalRegister.s[2], MipsPhysicalRegister.s[3],
            MipsPhysicalRegister.s[4], MipsPhysicalRegister.s[5], MipsPhysicalRegister.s[6], MipsPhysicalRegister.s[7],
            MipsPhysicalRegister.v1, MipsPhysicalRegister.t[8], MipsPhysicalRegister.t[9],
            MipsPhysicalRegister.gp  // Hey, more registers!
        );
        // 临时寄存器，用于从栈中加载/存储溢出的寄存器，最多同时使用两个，因此选定 $k0 与 $k1
        // 使用时，通过异或操作来切换，确保连续使用的两个不一样，以避免冲突
        static final List<MipsPhysicalRegister> temporaryRegisters = Arrays.asList(MipsPhysicalRegister.k0, MipsPhysicalRegister.k1);
    }

    private final MipsFunction function;
    private final HashMap<MipsBlock, Set<MipsVirtualRegister>> localVirtualRegisters;

    // 分配了物理寄存器的虚拟寄存器
    private final Map<MipsVirtualRegister, MipsPhysicalRegister> allocated = new HashMap<>();
    // 溢出的虚拟寄存器
    private final Set<MipsVirtualRegister> spilled = new HashSet<>();
    // 已经使用了的全局物理寄存器，在分配时，优先使用已经用过的
    private final Set<MipsPhysicalRegister> allocatedGlobal = new HashSet<>();

    private boolean haveNotAllocated(MipsVirtualRegister register) {
        return !allocated.containsKey(register) && !spilled.contains(register);
    }

    public MipsFunctionRegisterAllocate(MipsFunction function) {
        this.function = function;
        this.localVirtualRegisters = new HashMap<>();
        if (MipsFunction.isCaller(function)) {
            // 如果函数会调用其他函数，那么 ra 寄存器必须被标记为分配，也即需要保存/恢复
            this.allocatedGlobal.add(MipsPhysicalRegister.ra);
        }
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
                        // 这个虚拟寄存器首次出现
                        records.put(register, new Pair<>(block, blockLevel));
                    } else {
                        Pair<MipsBlock, Integer> record = records.get(register);
                        // 已经出现过，检查是否在同一个块中
                        if (record == null || !record.equals(new Pair<>(block, blockLevel))) {
                            // 但凡出过一次问题，就标记为 null，不分配局部寄存器
                            records.put(register, null);
                        }
                    }
                }
                if (instruction instanceof MipsIJump || instruction instanceof MipsIBrZero || instruction instanceof MipsIBrEqu) {
                    // 它们都会存在破坏局部寄存器的可能性，因此需要增加 blockLevel
                    blockLevel++;
                }
            }
        }

        /// Those who are just the local registers must be defined in the same block and before every use.
        /// Otherwise, when entering the block the first time, the register will be undefined.

        for (var entry : records.entrySet()) {
            if (entry.getValue() != null) {
                // 所有标记过的，且最后不为 null 的，都分配局部寄存器
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

            // 首先考察一下每个虚拟寄存器的活跃区间，重点是最后一次活跃的位置（指令）
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

            // 目前已经分配的寄存器所对应的结束活跃的指令
            HashMap<MipsPhysicalRegister, MipsInstruction> currentAllocateMap = new HashMap<>();
            for (MipsPhysicalRegister register : Config.localRegisters) {
                currentAllocateMap.put(register, null);
            }
            LoopInstruction: for (DualLinkedList.Node<MipsInstruction> iNode : block.getInstructions()) {
                MipsInstruction instruction = iNode.getData();
                // 遍历全部操作数，检查是否需要释放
                for (MipsOperand operand : instruction.getOperands()) {
                    MipsVirtualRegister register = fitLocalAllocate(operand, block);
                    if (register == null) {
                        continue;
                    }
                    if (allocated.containsKey(register)) {
                        // 这里表示已经分配了物理寄存器，检查是否需要释放
                        if (currentAllocateMap.get(allocated.get(register)) == instruction) {
                            // Release the register
                            currentAllocateMap.put(allocated.get(register), null);
                        }
                    }
                }
                // 遍历全部操作数，检查是否需要分配
                for (MipsOperand operand : instruction.getOperands()) {
                    MipsVirtualRegister register = fitLocalAllocate(operand, block);
                    if (register == null) {
                        continue;
                    }
                    if (spilled.contains(register) || allocated.containsKey(register)) {
                        // 已经溢出或者已经分配了物理寄存器，不再分配
                        continue;
                    }
                    // Allocate the register
                    for (MipsPhysicalRegister physicalRegister : Config.localRegisters) {
                        if (currentAllocateMap.get(physicalRegister) == null) {
                            if (lastLivingMap.get(register) != instruction) {
                                // 设置为寄存器结束活跃的指令
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
                        // Do not add the edge if they are in the same block directly
                        // This is a very slow implementation, but it is enough for the current situation
                        boolean intersect = false;
                        for (DualLinkedList.Node<MipsInstruction> iNode : block.getInstructions()) {
                            MipsInstruction instruction = iNode.getData();
                            if (Arrays.stream(instruction.getOperands()).anyMatch(
                                operand -> operand instanceof MipsVirtualRegister r && r == other
                            )) {
                                intersect = true;
                                break;
                            }
                        }
                        if (!intersect) {
                            interferenceGraph.addEdge(register, other);
                        }
                    }
                }
            }
        }
        // 4. Prepare the data structure for the graph coloring
        int K = Config.globalRegisters.size();
        Deque<MipsVirtualRegister> removed = new LinkedList<>();
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
                removed.addFirst(register);
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
