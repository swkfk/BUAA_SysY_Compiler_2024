package top.swkfk.compiler;

import top.swkfk.compiler.arch.ArchModule;
import top.swkfk.compiler.arch.mips.MipsModule;
import top.swkfk.compiler.error.ErrorTable;
import top.swkfk.compiler.frontend.Lexer;
import top.swkfk.compiler.frontend.Parser;
import top.swkfk.compiler.frontend.Traverser;
import top.swkfk.compiler.frontend.ast.CompileUnit;
import top.swkfk.compiler.frontend.symbol.SymbolTable;
import top.swkfk.compiler.frontend.token.TokenStream;
import top.swkfk.compiler.llvm.IrBuilder;
import top.swkfk.compiler.llvm.IrModule;
import top.swkfk.compiler.helpers.ParserWatcher;
import top.swkfk.compiler.llvm.analysises.AnalyseControlFlowGraph;
import top.swkfk.compiler.llvm.analysises.AnalyseDominatorTree;
import top.swkfk.compiler.llvm.analysises.AnalyseLoop;
import top.swkfk.compiler.llvm.transforms.AggressiveDeadCodeEliminate;
import top.swkfk.compiler.llvm.transforms.ConstantFolding;
import top.swkfk.compiler.llvm.transforms.ControlFlowSimplify;
import top.swkfk.compiler.llvm.transforms.DeadBlockEliminate;
import top.swkfk.compiler.llvm.transforms.Dummy;
import top.swkfk.compiler.llvm.transforms.LoopHoist;
import top.swkfk.compiler.llvm.transforms.MemoryToRegister;
import top.swkfk.compiler.llvm.transforms.MultiplySimplify;
import top.swkfk.compiler.llvm.transforms.ReadonlyGlobalEliminate;
import top.swkfk.compiler.llvm.transforms.VariableRename;

import java.io.FileWriter;
import java.io.IOException;

final public class Controller {
    public static final ErrorTable errors = new ErrorTable();
    public static final SymbolTable symbols = new SymbolTable();

    public static void run() throws IOException {
        // 1. Lexical analysis
        // 阶段一：词法分析，读取源文件，生成 Token 流。
        TokenStream tokens = new Lexer(Configure.source).lex().emit();
        if (Configure.debug.displayTokens) {
            System.err.println(tokens.toDebugString());
        }
        if (HomeworkConfig.hw == HomeworkConfig.Hw.Lexer) {
            try (FileWriter writer = new FileWriter(errors.noError() ? Configure.target : Configure.error)) {
                writer.write((errors.noError() ? tokens : errors).toString());
            }
            Controller.exit();
        }

        // 2. Syntax analysis
        // 阶段二：语法分析，从 Token 流中生成 AST.
        // 借助 ParserWatcher 类，可以在语法分析的过程中记录需要输出的内容。
        if (HomeworkConfig.hw == HomeworkConfig.Hw.Syntax) {
            Configure.debug.displayTokensWithAst = true;
        }
        ParserWatcher watcher = new ParserWatcher();
        CompileUnit ast = new Parser(tokens, watcher).parse().emit();
        if (HomeworkConfig.hw == HomeworkConfig.Hw.Syntax) {
            try (FileWriter writer = new FileWriter(errors.noError() ? Configure.target : Configure.error)) {
                writer.write((errors.noError() ? watcher : errors).toString());
            }
            Controller.exit();
        }

        // 3. Semantic analysis
        // 阶段三：语义分析，对 AST 进行遍历，生成符号表。符号表位于 Controller 类中。
        new Traverser(ast).spawn();  // --> SymbolTable
        if (HomeworkConfig.hw == HomeworkConfig.Hw.Semantic) {
            try (FileWriter writer = new FileWriter(errors.noError() ? Configure.target : Configure.error)) {
                writer.write((errors.noError() ? symbols.toString() : errors).toString());
            }
            Controller.exit();
        }

        // 对前期的错误进行输出，并截断后续的处理。
        if (!errors.noError()) {
            try (FileWriter writer = new FileWriter(Configure.error)) {
                writer.write("" + errors);
            }
            Controller.exit();
        }

        // 4. Intermediate code generation
        // 阶段四：生成中间代码，从 AST 中生成 LLVM IR.
        IrModule module = new IrBuilder(ast).build().emit();

        // 5. Intermediate code optimization
        // 阶段五：优化中间代码，对 IR 进行分析与变形。
        if (Configure.optimize) {
            module
                .runPass(new Dummy())
                .runPass(new AnalyseControlFlowGraph())
                .runPass(new DeadBlockEliminate())
                .runPass(new AnalyseDominatorTree())
                .runPass(new MemoryToRegister())
                .runPass(new ReadonlyGlobalEliminate())
                .runPass(new AggressiveDeadCodeEliminate())
                .runPass(new AnalyseLoop())
                .runPass(new LoopHoist())
                .runPass(new AnalyseControlFlowGraph())
                .runPass(new ConstantFolding())
                .runPass(new MultiplySimplify())
                .runPass(new AggressiveDeadCodeEliminate())
                .runPass(new ControlFlowSimplify())
                // Let's talk about the LVN.
                // LVN pass will make the temporary variable's lifetime longer, which will make the
                // register allocation more difficult. Especially when using the local register allocation.
                // When I shut down the LVN pass, the performance is better.
                // .runPass(new LocalVariableNumbering())
                .runPass(new AnalyseControlFlowGraph())
                .runPass(new AnalyseDominatorTree())
                .runPass(new AggressiveDeadCodeEliminate())
                .runPass(new ControlFlowSimplify())
                // Run the AggressiveDeadCodeEliminate pass again to eliminate more dead code caused by the dead block elimination
                .runPass(new AnalyseControlFlowGraph())
                .runPass(new AnalyseDominatorTree())
                .runPass(new AggressiveDeadCodeEliminate())
            ;
        }

        // 如果作业选择为 CodegenI，那么直接输出优化后的 IR，在此之前，会跑一遍
        // VariableRename Pass，以保证输出 LLVM 的正确性。
        if (HomeworkConfig.hw == HomeworkConfig.Hw.CodegenI) {
            if (Configure.optimize) {
                module.runPass(new VariableRename());
            }
            try (FileWriter writer = new FileWriter(Configure.target)) {
                writer.write(module.toString());
            }
            Controller.exit();
        }

        // 如果参数选择输出优化后的 IR，那么在此输出，同样会跑一遍 VariableRename Pass.
        if (Configure.debug.dumpOptimizedIr) {
            try (FileWriter writer = new FileWriter(Configure.dumpTarget)) {
                writer.write(module.runPass(new VariableRename()).toString());
            }
        }

        // 6. Machine Code generation
        // 阶段六：生成机器码，从 IR 中生成目标代码。试图进行多架构支持，但效果并不好。
        // 这里会执行 RemovePhi 操作，并进行虚拟寄存器层面的优化。
        ArchModule arch = (switch (Configure.arch) {
            case mips -> new MipsModule();
        })
            .runParseIr(module)
            .runRemovePhi()
            .runVirtualOptimize();

        // 如果参数选择输出虚拟寄存器的 MIPS 代码，那么在此输出。
        if (Configure.debug.dumpVirtualMips) {
            try (FileWriter writer = new FileWriter(Configure.dumpVirtualTarget)) {
                writer.write(arch.toString());
            }
        }

        // 执行寄存器分配与物理寄存器层面的优化。
        arch = arch
            .runRegisterAllocation()
            .runPhysicalOptimize();

        // 最后输出目标代码。
        if (HomeworkConfig.hw == HomeworkConfig.Hw.CodegenII) {
            try (FileWriter writer = new FileWriter(Configure.target)) {
                writer.write(arch.toString());
            }
            Controller.exit();
        }
    }

    private static void exit() {
        if (Configure.debug.displayErrors) {
            System.err.println("==> Errors: ");
            System.err.println(errors.toDebugString());
        }
        if (Configure.debug.displaySymbols) {
            System.err.println(symbols.toDebugString());
        }

        System.exit(0);
    }
}
