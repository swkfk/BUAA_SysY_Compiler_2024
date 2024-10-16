package top.swkfk.compiler.utils;

import java.util.Stack;

final public class BackTrace {

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

    public void save() {
        Object[] state = new Object[objects.length];
        for (int i = 0; i < objects.length; i++) {
            state[i] = objects[i].save();
        }
        states.push(state);
    }

    public void restore() {
        Object[] state = states.pop();
        for (int i = 0; i < objects.length; i++) {
            objects[i].restore(state[i]);
        }
    }
}
