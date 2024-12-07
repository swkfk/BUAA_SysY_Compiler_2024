package top.swkfk.compiler.llvm.transforms;

import top.swkfk.compiler.frontend.symbol.type.TyPtr;
import top.swkfk.compiler.llvm.IrModule;
import top.swkfk.compiler.llvm.Pass;
import top.swkfk.compiler.llvm.value.BasicBlock;
import top.swkfk.compiler.llvm.value.Function;
import top.swkfk.compiler.llvm.value.User;
import top.swkfk.compiler.llvm.value.Value;
import top.swkfk.compiler.llvm.value.constants.ConstInteger;
import top.swkfk.compiler.llvm.value.instruction.IAllocate;
import top.swkfk.compiler.llvm.value.instruction.ILoad;
import top.swkfk.compiler.llvm.value.instruction.IPhi;
import top.swkfk.compiler.llvm.value.instruction.IStore;
import top.swkfk.compiler.utils.DualLinkedList;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Replace memory operations with register operations. Only for integer types. <br>
 * Original Reference: <a href="https://roife.github.io/posts/mem2reg-pass/">Roife Blog</a> ;<br>
 * Direct Reference: <a href="https://gitlab.eduxiji.net/educg-group-26173-2487151/T202410006203104-3288/-/blame/stable/src/pass/ir/Mem2Reg.java">compiler2024-x</a>
 * , whose author is the same as the author of this project.
 */
final public class MemoryToRegister extends Pass {
    @Override
    public String getName() {
        return "mem2reg";
    }

    @Override
    public void run(IrModule module) {
        for (Function function : module.getFunctions()) {
            initializeAllocates(function);
            initializeDominators(function);
            placePhiInstruction();
            renameVariables(function);
        }
    }

    private final List<Value> allocates = new LinkedList<>();
    private final Map<IPhi, Value> newPhis = new HashMap<>();
    private final Map<Value, Set<BasicBlock>> allocateDefines = new HashMap<>();
    private final Map<BasicBlock, List<BasicBlock>> dominatorFrontiers = new HashMap<>();

    private void initializeAllocates(Function function) {
        allocates.clear();
        allocateDefines.clear();

        for (DualLinkedList.Node<BasicBlock> bNode : function.getBlocks()) {
            BasicBlock block = bNode.getData();
            if (!function.cfg.get().contains(block)) {
                continue;
            }
            for (DualLinkedList.Node<User> iNode : block.getInstructions()) {
                User instruction = iNode.getData();
                if (instruction instanceof IAllocate allocate) {
                    if (((TyPtr) allocate.getType()).getBase().is("int")) {
                        allocates.add(allocate);
                    }
                }
                if (instruction instanceof IStore store) {
                    if (allocates.contains(store.getPointer())) {
                        allocateDefines.computeIfAbsent(store.getPointer(), k -> new LinkedHashSet<>()).add(bNode.getData());
                    }
                }
            }
        }
    }

    private void initializeDominators(Function function) {
        dominatorFrontiers.clear();
        Deque<BasicBlock> workList = new LinkedList<>();
        HashSet<BasicBlock> visited = new HashSet<>();

        workList.push(function.getBlocks().getHead().getData());
        while (!workList.isEmpty()) {
            BasicBlock block = workList.pop();
            if (visited.contains(block)) {
                continue;
            }
            dominatorFrontiers.put(block, new LinkedList<>());
            visited.add(block);
            for (BasicBlock successor : function.cfg.get().getSuccessors(block)) {
                BasicBlock x = block;
                while (x != null && !function.dom.get().isAncestor(x, successor)) {
                    dominatorFrontiers.get(x).add(successor);
                    x = function.dom.get().getImmediateDominator(x);
                }
                workList.push(successor);
            }
        }
    }

    private void placePhiInstruction() {
        newPhis.clear();
        for (Value allocate : allocates) {
            Set<BasicBlock> visited = new HashSet<>();
            Deque<BasicBlock> workList = new LinkedList<>(allocateDefines.getOrDefault(allocate, new LinkedHashSet<>()));
            while (!workList.isEmpty()) {
                BasicBlock block = workList.pop();
                for (BasicBlock df : dominatorFrontiers.get(block)) {
                    if (!visited.contains(df)) {
                        visited.add(df);
                        IPhi phi = new IPhi(((TyPtr) allocate.getType()).getBase());
                        new DualLinkedList.Node<User>(phi).insertBefore(df.getInstructions().getHead());
                        newPhis.put(phi, allocate);
                        workList.push(df);
                    }
                }
            }
        }
    }

    private void renameVariables(Function function) {
        Set<BasicBlock> visited = new HashSet<>();
        Map<Value, Value> replaceMap = new HashMap<>();

        Deque<Map<BasicBlock, Map<Value, Value>>> workList = new LinkedList<>();
        workList.push(Map.of(function.getBlocks().getHead().getData(), new HashMap<>()));
        while (!workList.isEmpty()) {
            var entry = workList.pop();
            BasicBlock block = entry.keySet().iterator().next();
            Map<Value, Value> incoming = entry.get(block);
            if (visited.contains(block)) {
                continue;
            }
            visited.add(block);
            Iterator<DualLinkedList.Node<User>> iter = block.getInstructions().iterator();
            while (iter.hasNext()) {
                User instruction = iter.next().getData();
                for (var replaceEntry : replaceMap.entrySet()) {
                    instruction.replaceOperand(replaceEntry.getKey(), replaceEntry.getValue());
                }
                if (instruction instanceof IAllocate allocate) {
                    if (!allocates.contains(allocate)) {
                        continue;
                    }
                    iter.remove();
                }
                if (instruction instanceof ILoad load) {
                    if (!allocates.contains(load.getPointer())) {
                        continue;
                    }
                    replaceMap.put(load, incoming.get(load.getPointer()));
                    iter.remove();
                }
                if (instruction instanceof IStore store) {
                    if (!allocates.contains(store.getPointer())) {
                        continue;
                    }
                    incoming.put(store.getPointer(), store.getValue());
                    iter.remove();
                }
                if (instruction instanceof IPhi phi) {
                    if (!newPhis.containsKey(phi)) {
                        continue;
                    }
                    Value allocate = newPhis.get(phi);
                    incoming.put(allocate, phi);
                }
            }
            for (BasicBlock successor : function.cfg.get().getSuccessors(block)) {
                workList.push(Map.of(successor, new HashMap<>(incoming)));
                for (DualLinkedList.Node<User> iNode : successor.getInstructions()) {
                    User instruction = iNode.getData();
                    if (instruction instanceof IPhi phi) {
                        if (newPhis.containsKey(phi)) {
                            Value allocate = newPhis.get(phi);
                            if (incoming.containsKey(allocate)) {
                                phi.addIncoming(block, incoming.get(allocate));
                            } else {
                                phi.addIncoming(block, new ConstInteger(0, ((TyPtr) allocate.getType()).getBase()));
                            }
                        }
                    }
                }
            }
        }
        for (DualLinkedList.Node<BasicBlock> bNode : function.getBlocks()) {
            BasicBlock block = bNode.getData();
            if (!visited.contains(block)) {
                for (DualLinkedList.Node<User> iNode : block.getInstructions()) {
                    User instruction = iNode.getData();
                    for (Map.Entry<Value, Value> entry : replaceMap.entrySet()) {
                        instruction.replaceOperand(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }
}
