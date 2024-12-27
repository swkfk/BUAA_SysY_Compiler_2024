import top.swkfk.compiler.Configure;
import top.swkfk.compiler.Controller;
import top.swkfk.compiler.HomeworkConfig;

import java.io.IOException;

@SuppressWarnings("SpellCheckingInspection")
public class Compiler {
    /**
     * 主函数，解析参数并运行编译器。参数详情见 {@link Configure}，编译器运行流程见 {@link Controller}，
     * 作业切换请修改 {@link HomeworkConfig}。
     */
    public static void main(String[] args) throws IOException {
        Configure.parse(args);
        Controller.run();
    }

    // LLVM Clang 运行参数举例
    //   编译源文件：
    //     clang -S -emit-llvm testfile.c -o llvm_ir.ll
    //   链接生成的 LLVM IR 文件：
    //     llvm-link -S llvm_ir.ll lib.ll -o exe.ll
    //   仿真链接后的 LLVM IR 文件：
    //     lli exe.ll
    // Mars 运行参数举例
    //   编译 MIPS 汇编文件：
    //     java -jar Mars.jar testfile_out.mips

    // 上机需要注意的事项：
    //   1. 准备好 testcase 目录，放入测试用例
    //   2. 下载 SysY 语言的 lib.ll 文件，放在 testcase 目录下
    //   3. 关闭 Configure 中的默认优化，首先确保正确性
    //   4. 设置 IDEA 的运行参数，并编译运行 Compiler 生成目标代码等
    //   5. 运行 Archiver 打包，浏览器定位一下文件夹，方便提交
    //   6. 运行 Mars，可以的话定位到 testcase 目录，方便打开文件
    //   7. 试用一下 WSL 的 llvm 相关命令，进行测试，同时提供历史记录
    //   8. 调整 IDEA 设置，比如字体字号
    //   9. 最后开启优化，以期不会出错，同时提高性能
    
    // 运行参数举例：
    /*
        testcase/testfile_out.c
        -o testcase/testfile_out.mips
        -error testcase/error.txt
        -debug pass-debug
        -debug pass-verbose
        -debug opt-llvm
        -dump testcase/llvm_ir.ll
        -debug vir
        -dump-vir testcase/virtual.mips
        -debug errors
    */
}
