package top.swkfk.compiler.frontend.token;

import top.swkfk.compiler.frontend.Navigation;

import java.util.Arrays;

public class Token {
    private TokenType type;
    private String value;
    private Navigation location;

    public Token(TokenType type, String value, Navigation location) {
        this.type = type;
        this.value = value;
        this.location = location;
    }

    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public Navigation getLocation() {
        return location;
    }

    public String toString() {
        return type + " " + value;
    }

    public String toDebugString() {
        return type + " " + value + " " + location;
    }

    /**
     * Whether the token has the same content as the given token.
     * @param tk the token to compare
     * @return result
     */
    public boolean is(Token tk) {
        return type == tk.type && value.equals(tk.value);
    }

    /**
     * Whether the token has the same type as the given type.
     * @param type the type to compare
     * @return result
     */
    public boolean is(TokenType type) {
        return this.type == type;
    }

    /**
     * Whether the token has the same type as one of the given types.
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
