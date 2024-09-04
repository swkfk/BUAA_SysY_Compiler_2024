package top.swkfk.compiler;

import top.swkfk.compiler.frontend.Lexer;
import top.swkfk.compiler.frontend.token.TokenStream;

import java.io.FileWriter;
import java.io.IOException;

public class Controller {
    public static void frontend() throws IOException {
        TokenStream tokens = new Lexer(Configure.source).lex().emit();
        // Homework 2: Output the tokens
        try (FileWriter writer = new FileWriter(Configure.target)) {
            writer.write(tokens.toString());
        }
        System.exit(0);
        // Homework 2 ends
    }
}
