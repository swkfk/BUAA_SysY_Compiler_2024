package top.swkfk.compiler.frontend;

import top.swkfk.compiler.frontend.ast.CompileUnit;
import top.swkfk.compiler.frontend.token.TokenStream;
import top.swkfk.compiler.utils.ParserWatcher;

public class Parser {
    private final TokenStream tokens;
    private final ParserWatcher watcher;
    private final CompileUnit ast;

    public Parser(TokenStream tokens, ParserWatcher watcher) {
        this.tokens = tokens;
        this.watcher = watcher;
        this.ast = new CompileUnit();
    }

    public Parser parse() {
        return this;
    }

    public CompileUnit emit() {
        return ast;
    }
}
