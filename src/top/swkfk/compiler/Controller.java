package top.swkfk.compiler;

import top.swkfk.compiler.error.ErrorTable;
import top.swkfk.compiler.frontend.Lexer;
import top.swkfk.compiler.frontend.Parser;
import top.swkfk.compiler.frontend.Traverser;
import top.swkfk.compiler.frontend.ast.CompileUnit;
import top.swkfk.compiler.frontend.symbol.SymbolTable;
import top.swkfk.compiler.frontend.token.TokenStream;
import top.swkfk.compiler.utils.ParserWatcher;

import java.io.FileWriter;
import java.io.IOException;

public class Controller {
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
        ErrorTable errors = new ErrorTable();
        SymbolTable symbols = new SymbolTable();
        new Traverser(ast, errors, symbols).spawn();
        if (Configure.debug.displayErrors) {
            System.err.println(errors.toDebugString());
        }
        // <Homework 4>
        try (FileWriter writer = new FileWriter(Configure.target)) {
         writer.write(errors.toString());
        }
        System.exit(0);
        // </Homework 4>
    }
}
