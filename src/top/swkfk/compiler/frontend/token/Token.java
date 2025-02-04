package top.swkfk.compiler.frontend.token;

import top.swkfk.compiler.frontend.Lexer;
import top.swkfk.compiler.frontend.Navigation;

import java.util.Arrays;

/**
 * Token 流的元素，包含类型、值和位置信息。
 * @param type Token 类型
 * @param value Token 值
 * @param location 在源代码中的位置
 */
public record Token(TokenType type, String value, Navigation location) {

    public String toString() {
        String value = this.value;
        // We put the '\\' in the front of the escape characters to make it right.
        value = value.replace("\\", "\\\\");
        // 这里使用了一些很丑陋的方法来还原转义字符
        for (var entry : Lexer.escape.entrySet()) {
            if (entry.getKey() == '\\') {
                continue;
            }
            value = value.replace("" + entry.getValue(), "\\" + entry.getKey());
        }
        // 对字符串与字符添加相应的引号
        if (type == TokenType.StrConst) {
            value = "\"" + value + "\"";
        }
        if (type == TokenType.CharConst) {
            value = "'" + value + "'";
        }
        return type + " " + value;
    }

    public String toDebugString() {
        return this + " " + location;
    }

    /**
     * Whether the token has the same content as the given token.
     *
     * @param tk the token to compare
     * @return result
     */
    public boolean is(Token tk) {
        return type == tk.type && value.equals(tk.value);
    }

    /**
     * Whether the token has the same type as the given type.
     *
     * @param type the type to compare
     * @return result
     */
    public boolean is(TokenType type) {
        return this.type == type;
    }

    /**
     * Whether the token has the same type as one of the given types.
     *
     * @param types the types to compare
     * @return result
     */
    public boolean among(TokenType... types) {
        return Arrays.stream(types).anyMatch(this::is);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Token tk)) {
            return false;
        }
        return this.is(tk);
    }
}
