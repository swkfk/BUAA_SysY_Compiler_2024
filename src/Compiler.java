import top.swkfk.compiler.Configure;
import top.swkfk.compiler.Controller;

public class Compiler {
    public static void main(String[] args) {
        Configure.parse(args);
        Controller.frontend();
    }
}
