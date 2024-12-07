package top.swkfk.compiler.llvm;

import top.swkfk.compiler.Configure;

public abstract class Pass {
    private static int passCounter = 0;

    public abstract String getName();

    public int getPassID() {
        return ++passCounter;
    }

    public abstract void run(IrModule module);

    public void debug(String message) {
        if (Configure.debug.displayPassDebug) {
            System.out.println("<" + getName() + "> " + message);
        }
    }

    public boolean canPrintVerbose() {
        return true;
    }
}
