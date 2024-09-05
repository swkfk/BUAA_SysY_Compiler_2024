package top.swkfk.compiler.frontend.ast;

import top.swkfk.compiler.frontend.ast.declaration.function.FuncDef;
import top.swkfk.compiler.frontend.ast.declaration.function.MainFuncDef;
import top.swkfk.compiler.frontend.ast.declaration.object.Decl;

import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

final public class CompileUnit extends ASTNode {
    private final List<Decl> declarations = new LinkedList<>();
    private final List<FuncDef> functions = new LinkedList<>();
    private final MainFuncDef[] mainFunc = new MainFuncDef[1];

    public CompileUnit() {
    }

    public void addDeclaration(Decl decl) {
        declarations.add(decl);
    }

    public void addFunction(FuncDef func) {
        functions.add(func);
    }

    public void setMainFunc(MainFuncDef func) {
        mainFunc[0] = func;
    }

    @Override
    protected String getName() {
        return "<CompUnit>";
    }
}
