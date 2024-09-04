import top.swkfk.compiler.Configure;
import top.swkfk.compiler.Controller;

import java.io.IOException;

public class Compiler {
    public static void main(String[] args) throws IOException {
        Configure.parse(args);
        Controller.frontend();
    }
}
