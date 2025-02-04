package top.swkfk.compiler.arch.mips.process;

import top.swkfk.compiler.arch.mips.MipsBlock;
import top.swkfk.compiler.arch.mips.MipsFunction;
import top.swkfk.compiler.arch.mips.instruction.MipsIBinary;
import top.swkfk.compiler.arch.mips.instruction.MipsIJump;
import top.swkfk.compiler.arch.mips.instruction.MipsIPhi;
import top.swkfk.compiler.arch.mips.instruction.MipsInstruction;
import top.swkfk.compiler.arch.mips.operand.MipsImmediate;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.arch.mips.operand.MipsVirtualRegister;
import top.swkfk.compiler.utils.DualLinkedList;
import top.swkfk.compiler.utils.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

final public class MipsFunctionRemovePhi {
    /**
     * @deprecated See {@link MipsFunctionRemovePhi#removePhi()}.
     */
    static class ParallelCopy {
        /// Structure: <code>\[(dst, src)]</code>
        public List<Pair<MipsOperand, MipsOperand>> items = new LinkedList<>();
    }

    private final MipsFunction function;

    public MipsFunctionRemovePhi(MipsFunction function) {
        this.function = function;
    }

    public void run() {
        HashMap<MipsBlock, List<MipsInstruction>> toBeInserted = new HashMap<>();

        for (DualLinkedList.Node<MipsBlock> blockNode : function.getBlocks()) {
            MipsBlock currentBlock = blockNode.getData();
            HashMap<MipsBlock, MipsBlock> blockToReplace = new HashMap<>();

            for (DualLinkedList.Node<MipsInstruction> instructionNode : currentBlock.getInstructions()) {
                MipsInstruction instruction = instructionNode.getData();
                if (!(instruction instanceof MipsIPhi phi)) {
                    continue;
                }
                // 捕获到了一条 phi 指令，遍历它的每一对 incoming
                for (int i = 0; i < phi.getOperandsSize(); i++) {
                    MipsBlock incomingBlock = phi.getSource(i);
                    MipsOperand incomingValue = phi.getOperand(i);
                    if (incomingBlock.getSuccessors().size() == 1) {
                        // Only one successor, just use a move instruction. Pay attention to the temporary register used.
                        MipsOperand temporary = new MipsVirtualRegister();
                        toBeInserted.computeIfAbsent(incomingBlock, k -> new LinkedList<>()).addAll(List.of(
                            // Pay attention to the order of the following two instructions.
                            // When inserting, we should insert like 1st, 3rd, 5th, ..., 2nd, 4th, 6th, ...
                            buildMoveInstruction(temporary, incomingValue),
                            buildMoveInstruction(phi.getResult(), temporary)
                            // 上述的核心思想是，先将全部的 incomingValue 移动到对应的临时虚拟寄存器中，
                            // 然后再集中赋值，这是为了避免前一个赋值影响后一个赋值。所以才有了这样一个奇怪的插入顺序。
                            // 例如：$t0 <- $t1; $t2 <- $t0，这两个赋值应该“同时”进行，类似于 Verilog 的非阻塞赋值
                        ));
                    } else {
                        if (!blockToReplace.containsKey(incomingBlock)) {
                            // 新建一个块，用于存放 phi 指令的结果，同时避免重复创建
                            MipsBlock newBlock = new MipsBlock(incomingBlock + ".phi" + currentBlock);
                            // Modify the graph structure
                            MipsBlock.removeEdge(incomingBlock, currentBlock);
                            MipsBlock.addEdge(incomingBlock, newBlock);
                            MipsBlock.addEdge(newBlock, currentBlock);
                            // Replace the jump instruction
                            incomingBlock.getInstructions().forEach(iNode -> iNode.getData().replaceJumpTarget(currentBlock, newBlock));
                            // Update the records
                            blockToReplace.put(incomingBlock, newBlock);
                        }
                        MipsBlock newBlock = blockToReplace.get(incomingBlock);
                        newBlock.addInstruction(buildMoveInstruction(phi.getResult(), incomingValue));
                    }
                }
            }
            // 移除全部的 phi 指令
            while (currentBlock.getInstructions().getHead().getData() instanceof MipsIPhi) {
                currentBlock.getInstructions().getHead().drop();
            }
            // 新建的块，添加到函数中，并添加跳转指令（在最后添加，很好理解）
            for (MipsBlock newBlock : blockToReplace.values()) {
                function.addBlock(newBlock);
                newBlock.addInstruction(new MipsIJump(MipsIJump.X.j, currentBlock));
            }
        }
        // 按照上面说的奇怪的顺序插入直接移动指令
        for (var entry : toBeInserted.entrySet()) {
            DualLinkedList.Node<MipsInstruction> tail = entry.getKey().getInstructions().getTail();
            List<MipsInstruction> instructions = entry.getValue();
            for (int i = 0; i < instructions.size(); i += 2) {
                new DualLinkedList.Node<>(instructions.get(i)).insertBefore(tail);
            }
            for (int i = 1; i < instructions.size(); i += 2) {
                new DualLinkedList.Node<>(instructions.get(i)).insertBefore(tail);
            }
        }
    }

    /**
     * 生成移动指令，处理 src 的不同情况
     * @param dst 目标寄存器
     * @param src 源寄存器或立即数
     * @return 生成的移动指令
     */
    private static MipsInstruction buildMoveInstruction(MipsOperand dst, MipsOperand src) {
        if (src instanceof MipsImmediate) {
            return new MipsIBinary(MipsIBinary.X.addiu, dst, MipsPhysicalRegister.zero, src);
        } else {
            return new MipsIBinary(MipsIBinary.X.addiu, dst, src, new MipsImmediate(0));
        }
    }

    /**
     * @deprecated Old implementation from the tutorial which I cannot understand.
     * I kept it here because it spent me a lot of time.
     */
    void removePhi() {
        //! for BasicBlock in function which has phi
        for (DualLinkedList.Node<MipsBlock> node : function.getBlocks()) {
            if (!(node.getData().getInstructions().getHead().getData() instanceof MipsIPhi)) {
                continue;
            }
            Map<MipsBlock, ParallelCopy> pCopyMap = new HashMap<>();
            //! for incomingBlock in incomingBlocks
            for (MipsBlock incomingBlock : List.copyOf(node.getData().getPredecessors())) {
                ParallelCopy pCopy = new ParallelCopy();
                //! if incomingBlock has only one successor
                if (incomingBlock.getSuccessors().size() == 1) {
                    //! append a p-copy instruction to the end of incomingBlock (before jump)
                    pCopyMap.put(incomingBlock, pCopy);
                }
                //! else
                else {
                    //! create a new block B
                    MipsBlock newBlock = new MipsBlock(null, incomingBlock + ".pCopy");
                    function.addBlock(newBlock);
                    //! replace incomingBlock -> BasicBlock with incomingBlock -> B -> BasicBlock
                    MipsBlock.removeEdge(incomingBlock, node.getData());
                    MipsBlock.addEdge(incomingBlock, newBlock);
                    MipsBlock.addEdge(newBlock, node.getData());
                    incomingBlock.getInstructions().forEach(i -> i.getData().replaceJumpTarget(node.getData(), newBlock));
                    newBlock.addInstruction(new MipsIJump(MipsIJump.X.j, node.getData()));
                    //! append a p-copy instruction to the end of B
                    pCopyMap.put(newBlock, pCopy);
                    pCopyMap.put(incomingBlock, pCopy);
                }
                //! endif

                //! for phi(B_1:a_1, ..., B_n:a_n) in BasicBlock
                for (DualLinkedList.Node<MipsInstruction> instruction : node.getData().getInstructions()) {
                    if (!(instruction.getData() instanceof MipsIPhi phi)) {
                        break;
                    }
                    //! for each (B_i, a_i) of phi
                    for (int i = 0; i < phi.getOperandsSize(); i++) {
                        MipsBlock incoming = phi.getSource(i);
                        MipsOperand operand = phi.getOperand(i);
                        MipsOperand newOperand = new MipsVirtualRegister();
                        //! add a_i_sub <- a_i to p-copy_i
                        pCopyMap.get(incoming).items.add(new Pair<>(newOperand, operand));
                        //! replace a_i with a_i_sub in phi
                        phi.replaceOperand(i, newOperand);
                    }
                }
                System.err.println("pCopy: " + pCopy.items);
            }
        }
    }
}
