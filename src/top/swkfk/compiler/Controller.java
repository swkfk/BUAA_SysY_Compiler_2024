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
import top.swkfk.compiler.llvm.transforms.Dummy;
import top.swkfk.compiler.llvm.transforms.MemoryToRegister;

import java.io.FileWriter;
import java.io.IOException;

final public class Controller {
    public static final ErrorTable errors = new ErrorTable();
    public static final SymbolTable symbols = new SymbolTable();

    public static void run() throws IOException {
        // 1. Lexical analysis
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
        new Traverser(ast).spawn();  // --> SymbolTable
        if (HomeworkConfig.hw == HomeworkConfig.Hw.Semantic) {
            try (FileWriter writer = new FileWriter(errors.noError() ? Configure.target : Configure.error)) {
                writer.write((errors.noError() ? symbols.toString() : errors).toString());
            }
            Controller.exit();
        }

        // 4. Intermediate code generation
        IrModule module = new IrBuilder(ast).build().emit();
        if (HomeworkConfig.hw == HomeworkConfig.Hw.CodegenI) {
            try (FileWriter writer = new FileWriter(Configure.target)) {
                writer.write(module.toString());
            }
            Controller.exit();
        }

        // 5. Intermediate code optimization
        if (Configure.optimize) {
            module
                .runPass(new Dummy())
                .runPass(new AnalyseControlFlowGraph())
                .runPass(new AnalyseDominatorTree())
                .runPass(new MemoryToRegister())
            ;
        }

        if (Configure.debug.dumpOptimizedIr) {
            try (FileWriter writer = new FileWriter("llvm_ir.txt")) {
                writer.write(module.toString());
            }
        }

        // 6. Machine Code generation
        ArchModule arch = (switch (Configure.arch) {
            case mips -> new MipsModule();
        })
            .runParseIr(module)
            .runRemovePhi()
            .runVirtualOptimize()
            .runRegisterAllocation()
            .runPhysicalOptimize();
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
