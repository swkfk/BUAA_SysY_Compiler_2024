package top.swkfk.compiler.helpers;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.frontend.symbol.type.Ty;
import top.swkfk.compiler.frontend.symbol.type.TyPtr;
import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.llvm.value.constants.ConstInteger;
import top.swkfk.compiler.llvm.value.instruction.BinaryOp;
import top.swkfk.compiler.llvm.value.instruction.IComparator;
import top.swkfk.compiler.llvm.value.instruction.IConvert;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 处理各种令人讨厌的类型转换，并能够通过 builder 来插入新的指令
 */
final public class Compatibility {
    private static Function<User, Value> builder;

    public static void setBuilder(Function<User, Value> builder) {
        Compatibility.builder = builder;
    }

    /**
     * 将所有的值转换为布尔值
     * @param values 需要转换的值
     * @return 转换后的值，如果原值是布尔值则不会转换
     */
    public static Value[] unityIntoBoolean(Value... values) {
        return Stream.of(values).map(value -> {
            if (value.getType().is("i1")) {
                return value;
            } else {
                return builder.apply(new IComparator(BinaryOp.Ne, value, new ConstInteger(0, value.getType())));
            }
        }).toArray(Value[]::new);
    }

    /**
     * 将所有的值转换为整数（int）
     * @param values 需要转换的值
     * @return 转换后的值，如果原值是 i32 则不会转换
     */
    public static Value[] unityIntoInteger(Value... values) {
        return unityIntoInteger(Ty.I32, values);
    }

    /**
     * 将所有的值转换为指定的整数类型
     * @param target 目标类型（整数类型）
     * @param values 需要转换的值
     * @return 转换后的值，如果原值是目标类型则不会转换
     */
    public static Value[] unityIntoInteger(SymbolType target, Value... values) {
        return Stream.of(values).map(value -> {
            if (value.getType().equals(target)) {
                return value;
            } else {
                return builder.apply(new IConvert(target, value));
            }
        }).toArray(Value[]::new);
    }

    /**
     * 将所有的值转换为指针类型所指向的整数类型
     * @param values 需要转换的值
     * @return 转换后的值，如果原值已经是目标类型，则不会转换
     */
    public static Value[] unityIntoPointer(Value pointer, Value... values) {
        assert pointer.getType().is("ptr") : "Expected pointer type in compatibility";
        return unityIntoInteger(((TyPtr) pointer.getType()).getBase(), values);
    }
}
