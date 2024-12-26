package top.swkfk.compiler.arch.mips;

import top.swkfk.compiler.arch.mips.instruction.MipsIBinary;
import top.swkfk.compiler.arch.mips.instruction.MipsIJump;
import top.swkfk.compiler.arch.mips.instruction.MipsInstruction;
import top.swkfk.compiler.arch.mips.operand.MipsImmediate;
import top.swkfk.compiler.arch.mips.operand.MipsOperand;
import top.swkfk.compiler.arch.mips.operand.MipsPhysicalRegister;
import top.swkfk.compiler.utils.DualLinkedList;

final public class MipsFunction extends MipsOperand {
    private final String name;
    private final DualLinkedList<MipsBlock> blocks = new DualLinkedList<>();
    private MipsBlock entry, exit;
    private int stackSize = 0;

    public MipsFunction(String name) {
        this.name = name;
    }

    public int getStackSize() {
        return stackSize;
    }

    public void setStackSize(int stackSize) {
        this.stackSize = stackSize;
    }

    public void enlargeStackSize(int stackSize) {
        this.stackSize += stackSize;
    }

    public void setKeyBlock(MipsBlock entry, MipsBlock exit) {
        this.entry = entry;
        this.exit = exit;
    }

    public MipsBlock getEntryBlock() {
        return entry;
    }

    public MipsBlock getExitBlock() {
        return exit;
    }

    public void addBlock(MipsBlock block) {
        new DualLinkedList.Node<>(block).insertIntoTail(blocks);
    }

    public DualLinkedList<MipsBlock> getBlocks() {
        return blocks;
    }

    @Override
    public String toString() {
        return name;
    }

    public String toMips() {
        StringBuilder sb = new StringBuilder();
        // sb.append(name).append(":\n");
        for (DualLinkedList.Node<MipsBlock> node : blocks) {
            sb.append(node.getData().toMips()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 填充栈大小
     */
    public void fillStackSize() {
        // 在函数出口，最后一个指令（返回语句）之前插入栈大小的调整指令（回收栈）
        new DualLinkedList.Node<MipsInstruction>(
            new MipsIBinary(MipsIBinary.X.addiu, MipsPhysicalRegister.sp, MipsPhysicalRegister.sp, new MipsImmediate(getStackSize()))
        ).insertBefore(exit.getInstructions().getTail());
        // 在函数入口，第一个调整 $fp 的指令之后插入栈大小的调整指令（扩充栈），即使是 main 函数，也会有 $fp <- $sp 的指令
        entry.addInstructionAfter(
            new MipsIBinary(MipsIBinary.X.addiu, MipsPhysicalRegister.sp, MipsPhysicalRegister.sp, new MipsImmediate(-getStackSize())),
            instruction -> instruction instanceof MipsIBinary && instruction.getOperands()[0] == MipsPhysicalRegister.fp
        );
    }

    /**
     * 判断函数是否会调用其他函数
     * @param function 待检查的函数
     * @return 是否是调用者
     */
    public static boolean isCaller(MipsFunction function) {
        for (DualLinkedList.Node<MipsBlock> bNode : function.getBlocks()) {
            for (DualLinkedList.Node<MipsInstruction> iNode : bNode.getData().getInstructions()) {
                MipsInstruction instruction = iNode.getData();
                // 只有 jal 之类的指令才算调用其他函数
                if (instruction instanceof MipsIJump jump && jump.isCall()) {
                    return true;
                }
            }
        }
        return false;
    }
}
