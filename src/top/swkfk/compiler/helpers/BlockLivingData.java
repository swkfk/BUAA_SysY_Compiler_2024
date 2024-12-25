package top.swkfk.compiler.helpers;

import java.util.HashSet;
import java.util.Set;

/**
 * BlockLivingData, including use, def, in, out
 * 存储每个基本块的活跃变量信息
 *
 * @param <T> Normally it is Arch's Virtual Register Class
 */
final public class BlockLivingData<T> {
    private final Set<T> use, def, in, out;

    public BlockLivingData() {
        this.use = new HashSet<>();
        this.def = new HashSet<>();
        this.in = new HashSet<>();
        this.out = new HashSet<>();
    }

    public Set<T> getUse() {
        return use;
    }

    public Set<T> getDef() {
        return def;
    }

    public Set<T> getIn() {
        return in;
    }

    public Set<T> getOut() {
        return out;
    }

    public static <T> void minus(Set<T> inst, Set<T> other) {
        inst.removeAll(other);
    }

    public static <T> void union(Set<T> inst, Set<T> other) {
        inst.addAll(other);
    }

    @Override
    public String toString() {
        return "BlockLivingData{" +
            "use=" + use +
            ", def=" + def +
            ", in=" + in +
            ", out=" + out +
            '}';
    }
}
