package top.swkfk.compiler.llvm.transforms;

import top.swkfk.compiler.llvm.IrModule;
import top.swkfk.compiler.llvm.Pass;
import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.llvm.value.Function;
import top.swkfk.compiler.llvm.value.GlobalVariable;
import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.llvm.value.constants.ConstInteger;
import top.swkfk.compiler.llvm.value.instruction.IGep;
import top.swkfk.compiler.llvm.value.instruction.ILoad;
import top.swkfk.compiler.llvm.value.instruction.IStore;
import top.swkfk.compiler.utils.DualLinkedList;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

final public class ReadonlyGlobalEliminate extends Pass {
    @Override
    public String getName() {
        return "readonly-global-eliminate";
    }

    @Override
    public void run(IrModule module) {
        HashMap<Value, List<Integer>> readonly = recognize(module);
        HashMap<Value, Value> replace = new HashMap<>();
        for (Function function : module.getFunctions()) {
            for (DualLinkedList.Node<BasicBlock> bNode : function.getBlocks()) {
                BasicBlock block = bNode.getData();
                replace.putAll(detectReplaceMap(block, readonly));
            }
        }
        for (Function function : module.getFunctions()) {
            for (DualLinkedList.Node<BasicBlock> bNode : function.getBlocks()) {
                BasicBlock block = bNode.getData();
                replaceOperand(block, replace);
            }
        }
    }

    private HashMap<Value, List<Integer>> recognize(IrModule module) {
        HashMap<Value, List<Integer>> readonly = new HashMap<>();
        for (GlobalVariable gv : module.getGlobalVariables()) {
            if (!gv.getType().is("array")) {
                readonly.put(gv.getSymbol().getValue(), gv.getInitializerList());
            } else if (gv.getSymbol().isConst()) {
                readonly.put(gv.getSymbol().getValue(), gv.getInitializerList());
            }
        }
        for (Function function : module.getFunctions()) {
            for (DualLinkedList.Node<BasicBlock> bNode : function.getBlocks()) {
                BasicBlock block = bNode.getData();
                for (DualLinkedList.Node<User> iNode : block.getInstructions()) {
                    User instruction = iNode.getData();
                    for (Value operand : instruction.getOperands()) {
                        if (instruction instanceof IStore) {
                            readonly.remove(operand);
                        }
                    }
                }
            }
        }
        debug("Readonly global variables: " + readonly.keySet().stream().map(Value::getName).collect(Collectors.joining(", ")));
        return readonly;
    }

    private HashMap<Value, Value> detectReplaceMap(BasicBlock block, HashMap<Value, List<Integer>> readonly) {
        HashMap<Value, Value> replace = new HashMap<>();
        for (DualLinkedList.Node<User> iNode : block.getInstructions()) {
            User instruction = iNode.getData();
            if (instruction instanceof ILoad load) {
                Value pointer = load.getPointer();
                if (readonly.containsKey(pointer)) {
                    replace.put(instruction, new ConstInteger(readonly.get(pointer).get(0), instruction.getType()));
                }
            } else if (instruction instanceof IGep gep) {
                Value pointer = gep.getOperand(0);
                if (gep.getOperand(1) instanceof ConstInteger index && readonly.containsKey(pointer)) {
                    int idx = index.getValue();
                    if (iNode.getNext().getData() instanceof ILoad load && load.getPointer() == gep) {
                        replace.put(load, new ConstInteger(readonly.get(pointer).get(idx), load.getType()));
                    }
                }
            }
        }
        return replace;
    }

    private void replaceOperand(BasicBlock block, HashMap<Value, Value> replace) {
        for (DualLinkedList.Node<User> iNode : block.getInstructions()) {
            User instruction = iNode.getData();
            for (int i = 0; i < instruction.getOperands().size(); i++) {
                Value operand = instruction.getOperands().get(i);
                if (operand instanceof ILoad && replace.containsKey(operand)) {
                    instruction.replaceOperand(i, replace.get(operand));
                }
            }
        }
    }
}
