package top.swkfk.compiler;

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

import java.io.FileWriter;
import java.io.IOException;

public class Controller {
    public static final ErrorTable errors = new ErrorTable();
    public static final SymbolTable symbols = new SymbolTable();

    public static void frontend() throws IOException {
        // 1. Lexical analysis
        TokenStream tokens = new Lexer(Configure.source).lex().emit();
        if (Configure.debug.displayTokens) {
            System.err.println(tokens.toDebugString());
        }
        // <Homework 2: Output the tokens>
        // try (FileWriter writer = new FileWriter(Configure.target)) {
        //     writer.write(tokens.toString());
        // }
        // System.exit(0);
        // </Homework 2>

        // 2. Syntax analysis
        // <Homework 3: Output the tokens with its AST>
        // Configure.debug.displayTokensWithAst = true;
        // </Homework 3>
        ParserWatcher watcher = new ParserWatcher();
        CompileUnit ast = new Parser(tokens, watcher).parse().emit();
        // <Homework 3>
        // try (FileWriter writer = new FileWriter(Configure.target)) {
        //     writer.write(watcher.toString());
        // }
        // System.exit(0);
        // </Homework 3>

        // 3. Semantic analysis
        new Traverser(ast).spawn();  // --> SymbolTable
        if (Configure.debug.displayErrors) {
            System.err.println(errors.toDebugString());
        }
        if (Configure.debug.displaySymbols) {
            System.err.println(symbols);
        }
        // <Homework 4>
        // try (FileWriter writer = new FileWriter(Configure.target)) {
        //     writer.write(errors.toString());
        // }
        // System.exit(0);
        // </Homework 4>

        // 4. Intermediate code generation
        IrModule module = new IrBuilder(ast).build().emit();
        // <Homework 5>
        try (FileWriter writer = new FileWriter(Configure.target)) {
            writer.write(module.toString());
        }
        System.exit(0);
        // </Homework 5>
    }
}
