package top.swkfk.compiler.frontend.token;

import top.swkfk.compiler.utils.BackTrace;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Collect tokens and provide methods to access them.
 */
final public class TokenStream implements BackTrace.Traceable {
    private final List<Token> tks = new LinkedList<>();
    private int index = 0;

    /**
     * Whether the token stream is frozen. A frozen token stream cannot be modified.
     * Otherwise, it cannot be accessed and new tokens can come in.
     */
    private boolean frozen = false;

    /**
     * Freeze the token stream.
     */
    public void freeze() {
        assertNotFrozen();
        frozen = true;
    }

    private void assertFrozen() {
        if (!frozen) {
            throw new IllegalStateException("Cannot access token stream before freezing");
        }
    }

    private void assertNotFrozen() {
        if (frozen) {
            throw new IllegalStateException("Cannot access token stream after freezing");
        }
    }

    /**
     * Add a token to the stream before frozen.
     * @param tk the token to add
     */
    public void add(Token tk) {
        assertNotFrozen();
        tks.add(tk);
    }

    /**
     * Get the next token and move the cursor.
     * @return the next token
     */
    public Token next() {
        assertFrozen();
        if (index >= tks.size()) {
            return null;
        }
        return tks.get(index++);
    }

    /**
     * Consume a specific token. Otherwise, throw an exception.
     * @param type the token type to consume
     * @return the consumed token
     */
    public Token consume(TokenType type) {
        assertFrozen();
        Token nxt = peek();
        if (nxt == null || !nxt.is(type)) {
            throw new IllegalStateException("Expecting " + type + " but got " + nxt);
        }
        return next();
    }

    /**
     * Check if the next token is a specific type. If so, consume it and return true.
     * @param types the token types to check
     * @return <code>null</code> if the next token is not one of the types, otherwise the consumed token
     */
    public Token checkConsume(TokenType... types) {
        assertFrozen();
        if (!among(types)) {
            return null;
        }
        return next();
    }

    /**
     * Check if the next token is a specific type. If so, return true.
     * @param types the token types to check
     * @return whether the next token is one of the types
     */
    public boolean among(TokenType... types) {
        assertFrozen();
        return peek().among(types);
    }

    /**
     * Peek the <code>next</code>-th token.
     * @param next the number of tokens to peek, starting from 0
     * @return the <code>next</code>-th token
     */
    public Token peek(int next) {
        assertFrozen();
        if (index >= tks.size()) {
            return null;
        }
        return tks.get(index + next);
    }

    /**
     * Peek the next token.
     * @return the next token
     */
    public Token peek() {
        return peek(0);
    }

    /**
     * Whether the token stream is at the end.
     * @return whether the token stream is at the end
     */
    public boolean eof() {
        return peek() == null;
    }

    /**
     * Get the answer string of the homework.
     * @return each token one line, with the format <code>type value</code>
     */
    public String toString() {
        return tks.stream().map(Token::toString).collect(Collectors.joining("\n"));
    }

    /**
     * Get the debug string of the token stream.
     * @return each token one line, with the format <code>type value location</code>
     */
    public String toDebugString() {
        return tks.stream().map(Token::toDebugString).collect(Collectors.joining("\n"));
    }

    @Override
    public Object save() {
        return index;
    }

    @Override
    public void restore(Object state) {
        index = (int) state;
    }
}
