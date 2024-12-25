package top.swkfk.compiler.utils;

/**
 * 将两个不相干的类型封装在一起，只能保存并访问其中一个
 * @param <T> 左边的类型
 * @param <U> 右边的类型
 */
final public class Either<T, U> {
    private T left;
    private U right;
    private final boolean isLeft;

    private Either(T left, U right, boolean isLeft) {
        this.left = left;
        this.right = right;
        this.isLeft = isLeft;
    }

    public static <T, U> Either<T, U> left(T left) {
        return new Either<>(left, null, true);
    }

    public static <T, U> Either<T, U> right(U right) {
        return new Either<>(null, right, false);
    }

    public boolean isLeft() {
        return isLeft;
    }

    public boolean isRight() {
        return !isLeft;
    }

    public void setLeft(T left) {
        assert isLeft() : "Invalid Either side";
        this.left = left;
    }

    public void setRight(U right) {
        assert isRight() : "Invalid Either side";
        this.right = right;
    }

    public T getLeft() {
        return left;
    }

    public U getRight() {
        return right;
    }

    public String toString() {
        return isLeft ? left.toString() : right.toString();
    }
}
