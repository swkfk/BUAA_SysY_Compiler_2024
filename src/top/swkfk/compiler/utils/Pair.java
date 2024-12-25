package top.swkfk.compiler.utils;

/**
 * 将两个不相干的类型封装在一起，能保存并访问其中任意一个
 * @param first 第一个对象
 * @param second 第二个对象
 * @param <T> 第一个对象的类型
 * @param <U> 第二个对象的类型
 */
public record Pair<T, U>(T first, U second) {

    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    public boolean equals(Object obj) {
        if (obj instanceof Pair<?, ?> pair) {
            return first.equals(pair.first) && second.equals(pair.second);
        }
        return false;
    }

    public int hashCode() {
        return first.hashCode() ^ second.hashCode();
    }
}
