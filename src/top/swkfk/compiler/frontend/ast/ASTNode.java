package top.swkfk.compiler.frontend.ast;

/**
 * Abstract syntax tree node. All AST nodes will have the following properties:
 * <li>A display name</li>
 * <li>A start and an end position in the source code</li>
 */
abstract public class ASTNode {
    /**
     * Get the display name of the AST node.
     * @return The display name.
     */
    abstract protected String getName();

    public String toString() {
        return getName();
    }
}
