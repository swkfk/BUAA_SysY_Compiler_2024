package top.swkfk.compiler.helpers;

import top.swkfk.compiler.llvm.IrBuilder;
import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.llvm.value.constants.ConstInteger;
import top.swkfk.compiler.llvm.value.instruction.BinaryOp;
import top.swkfk.compiler.llvm.value.instruction.IComparator;

import java.util.function.Function;
import java.util.stream.Stream;

final public class Compatibility {
    private static Function<User, Value> builder;

    public static void setBuilder(Function<User, Value> builder) {
        Compatibility.builder = builder;
    }

    public static Value[] unityIntoBoolean(Value a, Value b) {
        return Stream.of(a, b).map(value -> {
            if (value.getType().is("i1")) {
                return value;
            } else {
                return builder.apply(new IComparator(BinaryOp.Ne, value, new ConstInteger(0, value.getType())));
            }
        }).toArray(Value[]::new);
    }
}
