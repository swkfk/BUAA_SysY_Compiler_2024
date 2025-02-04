package top.swkfk.compiler.helpers;

/**
 * 一个简易的存储注释的类，可以被挂载在一些类中
 */
final public class Comments {
    private String comment = "";
    private final String prefix;

    public Comments(String prefix) {
        this.prefix = prefix;
    }

    public Comments append(String comment) {
        this.comment += comment;
        return this;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public String toString() {
        if (comment.isEmpty()) {
            return "";
        }
        return prefix + comment;
    }
}
