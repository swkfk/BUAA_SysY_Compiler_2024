package top.swkfk.compiler.utils;

import top.swkfk.compiler.frontend.ast.expression.ExprAdd;
import top.swkfk.compiler.frontend.ast.expression.ExprMul;
import top.swkfk.compiler.frontend.token.TokenType;

final public class ConstValue {
    private int value;

    public ConstValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public enum CalcType {
        Add, Sub, Mul, Div, Mod
    }

    public static CalcType from(TokenType tt) {
        return switch (tt) {
            case Plus -> CalcType.Add;
            case Minus -> CalcType.Sub;
            case Mult -> CalcType.Mul;
            case Div -> CalcType.Div;
            case Mod -> CalcType.Mod;
            default -> null;
        };
    }

    public static CalcType from(ExprAdd.Op op) {
        return switch (op) {
            case ADD -> CalcType.Add;
            case SUB -> CalcType.Sub;
        };
    }

    public static CalcType from(ExprMul.Op op) {
        return switch (op) {
            case MUL -> CalcType.Mul;
            case DIV -> CalcType.Div;
            case MOD -> CalcType.Mod;
        };
    }

    public static int calculate(int aa, int bb, CalcType type) {
        return switch (type) {
            case Add -> aa + bb;
            case Sub -> aa - bb;
            case Mul -> aa * bb;
            case Div -> aa / bb;
            case Mod -> aa % bb;
        };
    }
}
