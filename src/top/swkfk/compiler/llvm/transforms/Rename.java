package top.swkfk.compiler.llvm.transforms;

import top.swkfk.compiler.helpers.GlobalCounter;
import top.swkfk.compiler.llvm.IrModule;
import top.swkfk.compiler.llvm.Pass;
import top.swkfk.compiler.llvm.value.Function;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.llvm.value.constants.ConstInteger;

import java.util.HashMap;

final public class Rename extends Pass {
    @Override
    public String getName() {
        return "rename";
    }

    @Override
    public void run(IrModule module) {
        module.getFunctions().forEach(this::run);
    }

    private final HashMap<String, String> map = new HashMap<>();
    private final GlobalCounter counter = new GlobalCounter();

    private void updateName(Value value) {
        if (value instanceof ConstInteger) {
            return;
        }
        if (value.getName() == null || value.getName().isEmpty() || value.getName().charAt(0) == '@') {
            return;
        }
        String raw = value.getName().replace("%", "");
        String prefix = value.getName().charAt(0) == '%' ? "%" : "";
        if (!map.containsKey(raw)) {
            map.put(raw, String.valueOf(counter.get()));
        }
        value.setName(prefix + map.get(raw));
    }

    private void run(Function function) {
        map.clear();
        counter.reset();
        function.getParams().forEach(this::updateName);
        function.getBlocks().forEach(basicBlockNode -> {
            this.updateName(basicBlockNode.getData());
            basicBlockNode.getData().getInstructions().forEach(instruction -> this.updateName(instruction.getData()));
        });
        debug(map.toString());
    }
}
