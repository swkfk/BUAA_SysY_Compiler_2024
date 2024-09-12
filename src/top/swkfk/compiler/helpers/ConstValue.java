package top.swkfk.compiler.helpers;

import top.swkfk.compiler.frontend.ast.expression.ExprAdd;
import top.swkfk.compiler.frontend.ast.expression.ExprMul;

final public class ConstValue {
    public enum CalcType {
        Add, Sub, Mul, Div, Mod
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
