package top.swkfk.compiler.utils;

import java.util.Objects;

/**
 * 简易容器类，用于保存一个对象的引用，可以通过 {@link #get()} 方法获取对象。重点在于可以设置
 * "invalidate"，使得对象引用失效。并提供了一些便捷的方法，如 {@link #getOrDefault(Object)} 等。
 * @param <T> 要保存的对象的类型
 */
final public class Container<T> {
    private T data;

    public Container() {
        this.data = null;
    }

    public Container(T data) {
        this.data = data;
    }

    public T get() {
        return Objects.requireNonNull(data);
    }

    public T getOrNull() {
        return data;
    }

    public T getOrDefault(T defaultValue) {
        return data == null ? defaultValue : data;
    }

    public void set(T data) {
        this.data = data;
    }

    public void invalidate() {
        this.data = null;
    }

    public boolean isPresent() {
        return data != null;
    }
}
