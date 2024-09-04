package top.swkfk.compiler.frontend.token;

import top.swkfk.compiler.frontend.Navigation;

import java.util.Arrays;

public record Token(TokenType type, String value, Navigation location) {

    public String toString() {
        return type + " " + value.replace("\n", "\\n");
    }

    public String toDebugString() {
        return type + " " + value.replace("\n", "\\n") + " " + location;
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
