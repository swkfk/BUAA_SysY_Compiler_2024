package top.swkfk.compiler.error;

public enum ErrorType {
    /// 非法 Token
    InvalidToken("a"),
    /// 非法格式化字符串，今年没有这个错误了
    InvalidFormatString("_"),
    /// 重复声明
    DuplicatedDeclaration("b"),
    /// 未定义引用
    UndefinedReference("c"),
    /// 参数数量不匹配
    MismatchedParameterCount("d"),
    /// 参数类型不匹配
    MismatchedParameterType("e"),
    /// 返回值类型不匹配
    MismatchedReturnType("f"),
    /// 缺少返回语句（需要返回的函数，最后一句不是 return 则会报这个错误）
    MissingReturnStatement("g"),
    /// 向常量赋值
    AssignToConstant("h"),
    /// 缺少分号
    ExpectedSemicolon("i"),
    /// 缺少右括号
    ExpectedRParen("j"),
    /// 缺少右方括号
    ExpectedRBracket("k"),
    /// 格式化字符串参数不匹配
    MismatchedFormatArgument("l"),
    /// 非法循环控制
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
