package top.swkfk.compiler.utils;

import java.util.Objects;

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
