package top.swkfk.compiler.utils;

final public class Either<T, U> {
    private final T left;
    private final U right;
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

    public T getLeft() {
        return left;
    }

    public U getRight() {
        return right;
    }
}
