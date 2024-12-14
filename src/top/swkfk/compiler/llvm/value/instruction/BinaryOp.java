package top.swkfk.compiler.llvm.value.instruction;

public enum BinaryOp {

    ADD("add"),
    SUB("sub"),
    MUL("mul"),
    @SuppressWarnings("SpellCheckingInspection")
    DIV("sdiv"),
    @SuppressWarnings("SpellCheckingInspection")
    MOD("srem"),
    AND("and"),
    OR("or"),
    XOR("xor"),
    SHL("shl"),
    @SuppressWarnings("SpellCheckingInspection")
    SHR("ashr"),

    Separator("===="),

    Eq("icmp eq"),
    Ne("icmp ne"),
    Lt("icmp slt"),
    Le("icmp sle"),
    Gt("icmp sgt"),
    Ge("icmp sge")
    ;

    private final String opcode;

    BinaryOp(String opcode) {
        this.opcode = opcode;
    }

    public String getOpcode() {
        return opcode;
    }

    public int calculate(int lhs, int rhs) {
        return switch (this) {
            case ADD -> lhs + rhs;
            case SUB -> lhs - rhs;
            case MUL -> lhs * rhs;
            case DIV -> lhs / rhs;
            case MOD -> lhs % rhs;
            case AND -> lhs & rhs;
            case OR -> lhs | rhs;
            case XOR -> lhs ^ rhs;
            case SHL -> lhs << rhs;
            case SHR -> lhs >> rhs;
            case Separator -> throw new RuntimeException("Separator is not a valid operator");
            case Eq -> lhs == rhs ? 1 : 0;
            case Ne -> lhs != rhs ? 1 : 0;
            case Lt -> lhs < rhs ? 1 : 0;
            case Le -> lhs <= rhs ? 1 : 0;
            case Gt -> lhs > rhs ? 1 : 0;
            case Ge -> lhs >= rhs ? 1 : 0;
        };
    }

    public boolean swappable() {
        return switch (this) {
            case ADD, MUL, AND, OR, XOR, Eq, Ne -> true;
            case SUB, DIV, MOD, Separator, Lt, Le, Gt, Ge, SHL, SHR -> false;
        };
    }
}
