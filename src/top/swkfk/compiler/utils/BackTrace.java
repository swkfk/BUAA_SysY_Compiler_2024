package top.swkfk.compiler.utils;

import java.util.Stack;

/**
 * 快速回溯工具类，用于保存和恢复对象的状态。对于需要回溯的对象，实现 {@link Traceable} 接口，
 * 定义保存和恢复状态的方法即可。在使用时，创建一个 {@link BackTrace} 对象，将需要回溯的对象传入，
 * 调用 {@link #save()} 方法保存状态，调用 {@link #restore()} 方法恢复状态。
 * 也可以使用 try-with-resources 语法，自动恢复状态。
 */
final public class BackTrace implements AutoCloseable {

    public interface Traceable {
        /**
         * Save the state of the traceable object.
         * @return the state defined by the object itself
         */
        Object save();

        /**
         * Restore the state of the traceable object.
         * @param state the state defined by the object itself, returned by {@link #save()}
         */
        void restore(Object state);
    }

    private final Traceable[] objects;
    private final Stack<Object[]> states = new Stack<>();

    public BackTrace(Traceable... objects) {
        this.objects = objects;
    }

    public BackTrace save() {
        Object[] state = new Object[objects.length];
        for (int i = 0; i < objects.length; i++) {
            state[i] = objects[i].save();
        }
        states.push(state);
        return this;
    }

    public void restore() {
        Object[] state = states.pop();
        for (int i = 0; i < objects.length; i++) {
            objects[i].restore(state[i]);
        }
    }

    @Override
    public void close() {
        restore();
    }
}
