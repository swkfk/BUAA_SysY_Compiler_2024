package top.swkfk.compiler.llvm.value.instruction;

import top.swkfk.compiler.frontend.symbol.type.SymbolType;
import top.swkfk.compiler.llvm.User;
import top.swkfk.compiler.llvm.value.Value;

final public class IConvert extends User {
    private final Value source;

    public IConvert(SymbolType target, Value value) {
        super("%" + Value.counter.get(), target);
        assert value.getType().is("int") && target.is("int") && !target.equals(value.getType()) :
            "Invalid conversion from " + value.getType() + " to " + target ;
        this.source = value;
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Override
    public String toLLVM() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append(" = ");

        if (getType().sizeof() < source.getType().sizeof()) {
            sb.append("trunc ");
        } else {
            sb.append("zext ");
        }

        sb.append(source).append(" to ").append(getType());
        return sb.toString();
    }
}
