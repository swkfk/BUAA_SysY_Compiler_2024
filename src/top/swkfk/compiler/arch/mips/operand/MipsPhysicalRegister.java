package top.swkfk.compiler.arch.mips.operand;

final public class MipsPhysicalRegister extends MipsOperand {
    private final int id;
    private final String name;

    /**
     * 构造函数，物理寄存器数目固定，不允许外部创建
     * @param id 寄存器编号
     * @param name 寄存器名称，带有 $ 符号
     */
    private MipsPhysicalRegister(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public static final MipsPhysicalRegister zero = new MipsPhysicalRegister(0, "$zero");
    public static final MipsPhysicalRegister at = new MipsPhysicalRegister(1, "$at");
    public static final MipsPhysicalRegister v0 = new MipsPhysicalRegister(2, "$v0");
    public static final MipsPhysicalRegister v1 = new MipsPhysicalRegister(3, "$v1");
    public static final MipsPhysicalRegister k0 = new MipsPhysicalRegister(26, "$k0");
    public static final MipsPhysicalRegister k1 = new MipsPhysicalRegister(27, "$k1");
    public static final MipsPhysicalRegister gp = new MipsPhysicalRegister(28, "$gp");
    public static final MipsPhysicalRegister sp = new MipsPhysicalRegister(29, "$sp");
    public static final MipsPhysicalRegister fp = new MipsPhysicalRegister(30, "$fp");
    public static final MipsPhysicalRegister ra = new MipsPhysicalRegister(31, "$ra");

    public static final MipsPhysicalRegister[] a = new MipsPhysicalRegister[] {
        new MipsPhysicalRegister(4, "$a0"), new MipsPhysicalRegister(5, "$a1"),
        new MipsPhysicalRegister(6, "$a2"), new MipsPhysicalRegister(7, "$a3")
    };

    public static final MipsPhysicalRegister[] t = new MipsPhysicalRegister[] {
        new MipsPhysicalRegister(8, "$t0"), new MipsPhysicalRegister(9, "$t1"),
        new MipsPhysicalRegister(10, "$t2"), new MipsPhysicalRegister(11, "$t3"),
        new MipsPhysicalRegister(12, "$t4"), new MipsPhysicalRegister(13, "$t5"),
        new MipsPhysicalRegister(14, "$t6"), new MipsPhysicalRegister(15, "$t7"),
        new MipsPhysicalRegister(24, "$t8"), new MipsPhysicalRegister(25, "$t9")
    };

    public static final MipsPhysicalRegister[] s = new MipsPhysicalRegister[] {
        new MipsPhysicalRegister(16, "$s0"), new MipsPhysicalRegister(17, "$s1"),
        new MipsPhysicalRegister(18, "$s2"), new MipsPhysicalRegister(19, "$s3"),
        new MipsPhysicalRegister(20, "$s4"), new MipsPhysicalRegister(21, "$s5"),
        new MipsPhysicalRegister(22, "$s6"), new MipsPhysicalRegister(23, "$s7")
    };

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MipsPhysicalRegister) {
            return id == ((MipsPhysicalRegister) obj).id;
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }
}
