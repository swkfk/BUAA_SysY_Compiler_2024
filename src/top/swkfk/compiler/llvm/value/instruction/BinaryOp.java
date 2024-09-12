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
}
