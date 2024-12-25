package top.swkfk.compiler.frontend;

import top.swkfk.compiler.Controller;
import top.swkfk.compiler.error.ErrorTable;
import top.swkfk.compiler.error.ErrorType;
import top.swkfk.compiler.frontend.token.Token;
import top.swkfk.compiler.frontend.token.TokenStream;
import top.swkfk.compiler.frontend.token.TokenType;
import top.swkfk.compiler.utils.Pair;

import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.util.Map;

import static java.util.Map.entry;

final public class Lexer {
    private final ErrorTable errors = Controller.errors;

    static class Reader {
        private final PushbackReader reader;
        private int lineno = 1;
        private int column = 0;
        /**
         * The column number of the last character read.
         */
        private int lastColumn = -1;

        public Reader(String source) throws IOException {
            reader = new PushbackReader(new FileReader(source));
        }

        public int read() throws IOException {
            int chr = reader.read();
            if (chr == '\n') {
                lineno++;
                lastColumn = column;
                column = 0;
            } else {
                column++;
            }
            return chr;
        }

        public void unread(int chr) throws IOException {
            if (chr == '\n') {
                lineno--;
                column = lastColumn;
            } else {
                column--;
            }
            reader.unread(chr);
        }

        public Pair<Integer, Integer> location() {
            return new Pair<>(lineno, column);
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    private final static Map<String, TokenType> keepwords = Map.ofEntries(
        entry("const", TokenType.Const), entry("int", TokenType.Int),
        entry("break", TokenType.Break), entry("continue", TokenType.Continue),
        entry("if", TokenType.If), entry("else", TokenType.Else),
        entry("for", TokenType.For), entry("return", TokenType.Return),
        entry("void", TokenType.Void), entry("main", TokenType.SpMain),
        entry("printf", TokenType.SpPrintf), entry("getint", TokenType.SpGetInt),
        entry("char", TokenType.Char), entry("getchar", TokenType.SpGetChar)
    );

    private final static Map<Character, TokenType> symbols = Map.ofEntries(
        entry('+', TokenType.Plus), entry('-', TokenType.Minus),
        entry('*', TokenType.Mult), entry('/', TokenType.Div), entry('%', TokenType.Mod),
        entry('{', TokenType.LBrace), entry('}', TokenType.RBrace),
        entry('[', TokenType.LBracket), entry(']', TokenType.RBracket),
        entry('(', TokenType.LParen), entry(')', TokenType.RParen),
        entry(';', TokenType.Semicolon), entry(',', TokenType.Comma)
    );

    /**
     * Escape characters. Pay attention to the '\\', which is an escape character itself.
     */
    public final static Map<Character, Character> escape = Map.ofEntries(
        entry('a', '\u0007'), entry('b', '\b'), entry('f', '\f'),
        entry('n', '\n'), entry('t', '\t'), entry('v', '\u000B'),
        entry('\\', '\\'), entry('\'', '\''), entry('"', '"'),
        entry('0', '\0')
    );

    private final Reader reader;
    private final TokenStream tokens = new TokenStream();

    public Lexer(String source) throws IOException {
        reader = new Reader(source);
    }

    /**
     * Lex the source file.
     * @return Self
     * @throws IOException if an I/O error occurs in read or unread
     */
    public Lexer lex() throws IOException {
        int chr;
        while ((chr = reader.read()) != -1) {
            // 跳过空白字符
            if (Character.isWhitespace(chr)) {
                continue;
            }
            // 这里就不是空白字符了，回退一个
            reader.unread(chr);

            if (Character.isDigit(chr)) {
                // 处理数字
                scanNumber();
            } else if (Character.isLetter(chr) || chr == '_') {
                // 处理标识符
                scanIdentifier();
            } else if (chr == '"') {
                // 读取字符串
                scanString();
            } else if (chr == '\'') {
                // 读取字符
                scanChar();
            } else if (chr == '/') {
                // 处理注释和除号
                scanSlash();
            } else {
                // 处理其他符号
                scanSymbol();
            }
        }
        return this;
    }

    private void scanSymbol() throws IOException {
        int chr = reader.read();
        Pair<Integer, Integer> start = reader.location();
        switch (chr) {
            // 这里重复代码较多，主要就是处理双字符符号
            case '|' -> {
                chr = reader.read();
                if (chr != '|') {
                    errors.add(ErrorType.InvalidToken, location());
                    reader.unread(chr);
                }
                addToken(TokenType.Or, "||", start);
            }
            case '&' -> {
                chr = reader.read();
                if (chr != '&') {
                    errors.add(ErrorType.InvalidToken, location());
                    reader.unread(chr);
                }
                addToken(TokenType.And, "&&", start);
            }
            case '!' -> {
                if ((chr = reader.read()) == '=') {
                    addToken(TokenType.Neq, "!=", start);
                } else {
                    reader.unread(chr);
                    addToken(TokenType.Not, "!", start);
                }
            }
            case '=' -> {
                if ((chr = reader.read()) == '=') {
                    addToken(TokenType.Eq, "==", start);
                } else {
                    reader.unread(chr);
                    addToken(TokenType.Assign, "=", start);
                }
            }
            case '<' -> {
                if ((chr = reader.read()) == '=') {
                    addToken(TokenType.Leq, "<=", start);
                } else {
                    reader.unread(chr);
                    addToken(TokenType.Lt, "<", start);
                }
            }
            case '>' -> {
                if ((chr = reader.read()) == '=') {
                    addToken(TokenType.Geq, ">=", start);
                } else {
                    reader.unread(chr);
                    addToken(TokenType.Gt, ">", start);
                }
            }
            default -> {
                // 剩下的单字符符号
                assert symbols.containsKey((char) chr) : "Invalid symbol `" + chr + "`.";
                addToken(symbols.get((char) chr), String.valueOf((char) chr), start);
            }
        }
    }

    private void scanIdentifier() throws IOException {
        StringBuilder identifier = new StringBuilder();
        int chr = reader.read();
        Pair<Integer, Integer> start = reader.location();
        while (Character.isLetterOrDigit(chr) || chr == '_') {
            identifier.append((char) chr);
            chr = reader.read();
        }
        reader.unread(chr);
        addToken(
            keepwords.getOrDefault(identifier.toString(), TokenType.Ident),
            identifier.toString(), start
        );
    }

    private void scanSlash() throws IOException {
        int chr = reader.read();
        assert chr == '/' : "Invariant broken";
        Pair<Integer, Integer> start = reader.location();

        chr = reader.read();
        if (chr == '/') {
            // Single-line comment
            while (chr != -1 && chr != '\n') {
                chr = reader.read();
            }
        } else if (chr == '*') {
            // Multi-line comment
            while ((chr = reader.read()) != -1) {
                int nxt = reader.read();
                if (chr == '*' && nxt == '/') {
                    break;
                }
                reader.unread(nxt);  // unread a char to avoid missing '*/'
            }
        } else {
            // Division
            reader.unread(chr);
            addToken(TokenType.Div, "/", start);
        }
    }

    private void scanNumber() throws IOException {
        // 读取数字，只会出现简单的整数形式，不会出现浮点数，也不允许前导零
        int chr = reader.read();
        Pair<Integer, Integer> start = reader.location();
        StringBuilder number = new StringBuilder();
        do {
            number.append((char) chr);
        } while (Character.isDigit(chr = reader.read()));
        reader.unread(chr);
        assert number.charAt(0) != '0' || number.length() == 1 : "Leading zero is not allowed.";
        addToken(TokenType.IntConst, number.toString(), start);
    }

    private void scanString() throws IOException {
        int chr = reader.read();
        assert chr == '"' : "Invariant broken";
        Pair<Integer, Integer> start = reader.location();
        StringBuilder str = new StringBuilder();
        while ((chr = reader.read()) != '"') {
            if (chr == '%') {
                // 原本是在这里处理格式化字符串的，但是今年不需要了，但也保留了特殊处理的分支
                chr = reader.read();
                str.append('%').append((char) chr);
            } else if (chr == '\\') {
                // 处理转义字符
                chr = reader.read();
                assert escape.containsKey((char) chr) : "Invalid escape character `\\" + chr + "`.";
                str.append(escape.get((char) chr));
            } else {
                // 通常字符，直接添加即可
                str.append((char) chr);
            }
        }
        addToken(TokenType.StrConst, str.toString(), start);
    }

    private void scanChar() throws IOException {
        int chr = reader.read();
        assert chr == '\'' : "Invariant broken";
        Pair<Integer, Integer> start = reader.location();
        chr = reader.read();
        if (chr == '\\') {
            // 处理转义字符
            chr = reader.read();
            assert escape.containsKey((char) chr) : "Invalid escape character `\\" + chr + "`.";
            chr = escape.get((char) chr);
        }
        int nxt = reader.read();
        assert nxt == '\'' : "Invalid character constant.";
        addToken(TokenType.CharConst, String.valueOf((char) chr), start);
    }

    /**
     * Freeze the token stream and return it.
     * @return The token stream
     */
    public TokenStream emit() {
        tokens.freeze();
        return tokens;
    }

    private void addToken(TokenType type, String value, Pair<Integer, Integer> start) {
        tokens.add(new Token(type, value, new Navigation(start, reader.location())));
    }

    private Navigation location() {
        return new Navigation(reader.location(), reader.location());
    }
}
