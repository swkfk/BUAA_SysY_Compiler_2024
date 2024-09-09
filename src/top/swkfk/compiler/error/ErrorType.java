package top.swkfk.compiler.error;

public enum ErrorType {
    InvalidFormatString("a"),
    DuplicatedDeclaration("b"),
    UndefinedReference("c"),
    MismatchedParameterCount("d"),
    MismatchedParameterType("e"),
    MismatchedReturnType("f"),
    MissingReturnStatement("g"),
    AssignToConstant("h"),
    ExpectedSemicolon("i"),
    ExpectedRParen("j"),
    ExpectedRBracket("k"),
    MismatchedFormatArgument("l"),
    InvalidLoopControl("m"),
    ;

    final private String type;

    ErrorType(String type) {
        this.type = type;
    }

    public String toString() {
        return type;
    }

    public String toDebugString() {
        return String.format("Error(%s, %s)", this.name(), type);
    }
}
