# BUAA-SysY-Compiler-2024 设计文档

> 22373080 沈锎

---

> **约定**：在文档中，以 `@` 代替 Java 软件包 `top.swkfk.compiler` 或者路径 `top/swkfk/compiler`，比如 `@.frontend.Lexer` 代表类 `top.swkfk.compiler.frontend.Lexer`，`@/Configure.java` 代表文件 `top/swkfk/compiler/Configure.java`，这是为了简化文档内容，并参考了 js 的相关约定。
>
> <https://swkfk.top> 是我的博客主页，遂有上述软件包名。

> **Whisper**：因为我在暑假参与了系统能力大赛的编译系统实现赛，并和队友实现了一个完整的 SysY 到 RISC-V 的编译器（<https://gitlab.eduxiji.net/educg-group-26173-2487151/T202410006203104-3288>），前端的全部任务由我负责，我同时也参与了中端优化与后端 Debug。因此，文档中所谓 **编码前的设计** 往往就是最终的设计，因为它们已经在暑假被我实践过了。**编码完成之后的修改** 也会侧重于对 bug 的查找与解决，而非架构的调整。

---

[TOC]

---

## 参考编译器介绍

在学习中，我阅读了参考资料中的 `pl0-compiler`，这是一个使用 Pascal 语言实现的 PL/0 编译器。

### 总体结构

这个编译器通过 (1)词法分析、(2)语法分析、(3)语义分析，(4)生成一种自定义的栈式语言，并内置了一个解释器进行 (5)解释执行。

参考编译器大体理应分成上述**五个部分**，但其中，词法分析、语法分析、语义分析与代码生成的内容存在一定的耦合。

#### 词法分析

参考编译器通过 **`getsym`** 过程读取下一个 token，并存储在**全局变量** `sym`（符号），`id`（标识符）与 `num`（数字）中。

以读取数字为例：

```pascal
Else If ch In ['0'..'9']
  Then
  Begin  { number }
    k := 0;
    num := 0;
    sym := number;
    Repeat
      num := 10*num+(ord(ch)-ord('0'));
      k := k+1;
      getch
    Until Not( ch In ['0'..'9']);
    If k > nmax
      Then error(30)
  End
```

从一个数字字符开始，一直读取，直到不是数字字符为止。在读取时，编译器同时维护了数字的实际值，并通过数字长度限制了不会越界。

在保存 `num` 时，将 `sym` 标记为 `number`，方便了后续的集中统一处理。

参考编译器并没有将词法分析与后面的部分分离，而是在进行后续翻译的过程中，向词法分析器**取用下一个 token**，在编译时，它们并不是严格分隔的过程。

#### 语法分析

参考编译器采用了**递归下降法**进行语法成分的分析。在主过程中，在读取了第一个 `sym` 之后，直接调用了对于 `block` 的语法分析过程：

```pascal
getsym;
block( 0,0,[period]+declbegsys+statbegsys );
```

进而处理其中的常量、变量、子过程、语句等。下列代码片段循环解析了一个 `block` 开头的常变量与子过程的声明，然后调用 `statement` 过程解析语句。

```pascal
Repeat
  If sym = constsym Then
    Begin
      getsym;
      Repeat
        constdeclaration;
        While sym = comma Do
          Begin
            getsym;
            constdeclaration
          End;
        If sym = semicolon
          Then getsym
        Else error(5)
      Until sym <> ident
    End;
  If sym = varsym Then
    { ... }
  While sym = procsym Do
    { ... }
  test( statbegsys+[ident],declbegsys,7 )
Until Not ( sym In declbegsys );
{ ... }
statement( [semicolon,endsym]+fsys);
{ ... }
```

在 `statement` 中，又根据符号种类的不同，调用了不同的语法成分的处理过程，实现了对整个源代码的处理。

### 接口设计

参考编译器采用**函数式**的写法，很多时候都采用全局变量进行信息的传递。

比如词法分析中，通过 `sym`，`id` 与 `num` 向语法分析器传递 token 信息。

在语法分析中，大部分的过程均有统一的参数，虽然这个并不能称之为接口，仅仅是程序员自己的约定而已：

```pascal
{ 部分过程举例 }
Procedure statement( fsys : symset );
Procedure expression( fsys: symset);
Procedure term( fsys : symset );
Procedure factor( fsys : symset );
Procedure expression( fsys: symset );
Procedure condition( fsys : symset );
```

> `symset` 是符号类型的集合，这个参数用来标记该过程中接受的符号集

### 文件组织

参考编译器只有**单文件**，不存在文件组织的讨论。

## 编译器总体设计

我实现的编译器将采用 Java 语言编写，并采用面向对象程序的设计模式进行代码组织与编写。

### 总体结构

#### 数据结构

**Token** 与 **Token 流**，记录词法分析器产生的全部 Token，并提供方法供语法分析器采用。

**抽象语法树 AST** 与 **语法树成员**，全部的语法树成员，它们均需要继承 `@.frontend.ast.ASTNode` 这一个抽象类，并为它们提供了题目要求的输出方法。

**符号 symbol**，存放在符号表（`@.frontend.symbol.SymbolTable`）中的表项，包含原名、类型、mangle 名以及其他属性。

> mangle 这一词来自 Rust 的相关内容，表示对原有符号名进行唯一混淆得到的新名字，这一举措可以化解不同作用域的同名符号。但后续使用并不充分，这其实是吸取了暑假开发的相关教训，**减少了通过“名字”进行特定内容识别的行为**。

**LLVM 中间代码**，以 `@.llvm.IrModule` 为核心进行组织，并依次有 `Function`，`BasicBlock` 与 `User`（指令）这一系列的层次结构，共同组成了 LLVM 中间代码的内容。

**代码优化**，并没有特定的数据结构，仅通过 `@.llvm.Pass` 抽象类，对各个 pass 进行了一个抽象，便于通过 `IrModule` 中的方法进行快速调用。

#### 编译过程

我实现编译器清晰地分为下列过程：

- **词法分析**（产生 `@.frontend.token.TokenStream` 供后续翻译过程使用）
- **语法分析**（产生 `@.frontend.ast.CompileUnit`，存储全局常变量与函数，实际上是语法树）
- **语义分析**（产生 `@.frontend.symbol.SymbolTable`，并将语法树中涉及到的符号与表项的引用绑定）
- **中间代码生成**（借助 `@.llvm.IrBuilder` 提供的辅助方法，通过 `@.llvm.Traverser` 进行语法树的构建，产生 `@.llvm.IrModule` 为中间代码的组织结构）
- **代码优化**（通过继承 `@.llvm.Pass` 抽象类定义 Pass，并在 `Controller` 中正确组织与排列）
- **目标代码生成**（依次执行 `@.arch.ArchModule` 中定义的一系列方法，进行 IR 解析、Phi 指令移除、虚拟寄存器优化、寄存器分配、物理寄存器优化，最终生产其对象，并通过 `toString()` 方法输出相关体系结构的代码）

### 接口设计

在设计各个类时，我进行了较为严格的访问权限管理，除了偶尔疏忽，不暴露给外部的方法均不使用 `public`，同时，兼顾到了给子类使用的 `protected` 以及包内可见。

- **词法解析** `@.frontend.Lexer`：暴露构造方法、`lex()` 方法（进行词法解析）与 `emit()` 方法（返回 Token 流）
- **语法分析** `@.frontend.Parser`：暴露构造方法、`parse()` 方法（进行语法分析）与 `emit()` 方法（返回编译单元对象，语法树）
  - 语法分析需要提供一个 `@.frontend.token.TokenStream` 对象，通过其提供的非常丰富的方法访问 Token
  - 语法分析需要提供一个 `@.helpers.ParserWatcher` 对象，通过其 `add(String)` 方法进行作业要求的输出
- **语义分析** `@.frontend.Traverser`：暴露构造方法、`spawn()` 方法（进行语义分析），产生符号表（`Controller.symbols`）与错误记录（`Controller.errors`）
- **中间代码生成** `@.llvm.Traverser`，通过给定的 `CompileUnit` 解析语法树，并借由 `@.llvm.IrBuilder` 提供的方法产生 `@.llvm.IrModule` 的 LLVM IR 结构
- **代码优化** 通过继承 `@.llvm.Pass` 接口定义 LLVM Pass，并在 `@.llvm.IrModule` 的 `runPass` 方法中被调用，实现对 LLVM IR 的分析与修改
- **目标代码生成** 通过 `@.arch.mips.process.MipsGenerator` 进行 Mips 代码的生成，以及 `@.arch.mips.process` 中的其他类与方法执行相应的操作

### 文件组织

- `Compiler.java`：编译器**入口**，调用方法解析参数，并调用相关过程。
- `@/Controller.java`：编译器的**控制中心**，承接编译过程中产生的对象，并根据配置选择执行内容。
- `@/Configure.java`，`@/HomeworkConfig.java`：对编译器的各项内容、调试、针对哪次作业进行配置。
- `@/helpers/`：存放**与编译器有关**的数据结构与算法内容。
- `@/utils/`：存放**与编译器无关**的数据结构。
- `@/error/`：对编译过程中产生的**错误**进行记录、输出的组件。
- `@/frontend/`：编译器**前端内容**：
  - `./ast/`：语法树及各级组件的定义
  - `./symbol/`：定义符号与符号表，以及各种奇怪的符号实体
  - `./token/`：定义 Token、类型、流等
  - `./Navigation.java`：提供记录元素在源代码中的位置信息
  - `./Lexer.java`：词法分析器，提取全部的 Token
  - `./Parser.java`：递归下降分析 Token，构建语法树
  - `./Traverser.java`：遍历语法树，构建符号表
- `@/llvm/`：**LLVM IR 相关内容**，包括结构定义与操作
  - `./IrModule.java`：LLVM IR 数据结构的顶层组织
  - `./IrBuilder.java`：LLVM IR 构建器的辅助代码
  - `./Traverser.java`：遍历语法树，产生 LLVM IR 的核心代码
  - `./Pass.java`：LLVM Pass 的基类
  - `./Use.java`：定义了 LLVM `Value` 之间的使用关系
  - `./value`：定义了 LLVM 的全部 `Value`，包括函数、基本块、常量、指令等
  - `./data_structure`：定义了优化需要使用到的数据结构
  - `./analysises`：定义了一系列不对 IR 进行修改，仅作分析的 Pass
  - `./transforms`：定义了对 IR 进行修改的 Pass
- `@/arch/mips`：**Mips 相关内容**，包括结构定义与操作
  - `./Mips*.java`：定义了 Mips 相关的数据结构
  - `./instruction`：定义了 Mips 中使用到的全部指令
  - `./operand`：定义了 Mips 中使用到的操作数
  - `./optimize`：对 Mips 进行直接优化的内容
  - `./process`：代码生成的主要过程，包括指令生成、Phi 指令移除、寄存器分配等

## 词法分析设计

此次任务要求我们编写一个词法分析器，从源代码中提取不同的 token，并对 `||` 与 `&&` 中的错误情况进行处理。

### 编码前的设计

#### 整体的代码组织结构

首先，按照题目要求，**主函数**位于源代码根目录，及 `Compiler.java`，这个改变不了。

其次，为了方面测试，以及切换作业，需要设计一些**全局的配置**，用于控制编译过程中的一些行为（比如是否输出什么信息啥的）。

因为编译器前后端之间存在一定的数据交互，它们本身也不止一步操作，因此需要一个**控制器**来进行整体的调控。这在我的设计中表现为 `@.Controller#frontend()` 与 `@.Controller#exit()` 等方法。

其余的，放在 `@` 的子包中，详见前文的 “文件组织”。

#### 数据结构

自然需要一个类（`@.frontend.token.Token`），用来表示每一个具体的 Token，需要记录**类型**、**表示值**、在源代码中的**位置**。同时，我也需要一些方法，来快捷地比较它们的类型，采用一些 Java 的特性，可以方便地进行编码。

同时，需要一个类（`@.frontend.token.TokenStream`）来承载全部的 Token，并为后续使用提供一些方便的方法，比如**提取并消耗**一个 Token、**检查类型**是否符合、**窥探**（peek）后面的 Token 等等。

也许还需要一个类，专门维护文件读取的状态（**当前行数**、列数），并提供 `read_char`，`unread_char` 以及 `is_end_of_file` 等功能。

#### 方法实现

它们通过词法解析器构建，词法分析器会一直读取字符，**直到文件结束**。在这个过程中，会**跳过空白符**，并根据首字符做出判断，进入不同的读取分支。

如果首字符是数字，则一直读取数字字符，并记录为整数；如果首字符是字母或者 `_`，则按照标识符的规矩读取，并将读取到的东西与保留字表进行对照，以判断是保留字还是标识符；如果首字符是引号，则读取字符串或者字符；如果首字符是 `/`，则需要进一步判断是否是注释；如果上述情况均不满足，则作为符号对待。

对于注释，我认为没必要设计成状态机，直接在函数内部将他们过滤即可。

对于双字符符号，则需要在读到特定字符时，再去判断下一个字符，来决定记录为哪一个符号，或者报错（比如只有 `|`）。

### 编码完成之后的修改

#### 架构变动

本次作业没有大的架构变动。

#### 调试与 Debug

- 字符串内部的 `\n` 导致的问题，一开始我在读取字符串时，将 `\n` 直接转变为换行符存储，导致打印时出现错误；
- 错误的使用 `Character.isSpaceChar(int)`，导致 `\r` 无法作为空白符被忽略，正确的应该使用 `Character.isWhitespace(int)`
- 在读取多行注释结束标志（`*/`）时，错误的使用下列代码进行，导致在读取 `**/` 时，第二个 `*` 被忽略而出现错误；

  ```diff
  - if (chr == '*' && reader.read() == '/') {
  + int nxt = reader.read();
  + if (chr == '*' && nxt == '/') {
        break;
    }
  + reader.unread(nxt);  // unread a char to avoid missing '*/'
  ```

## 语法分析设计

### 编码前的设计

#### 整体的代码组织结构

同词法分析一样，语法分析部分的数据结构与代码同样位于 `@.frontend` 中。整体采用**带有回溯的递归下降方法**完成。

#### 数据结构

考虑了使用内部类造成的类名过长的问题，此次设计中将每一个语法成分均设计出了一个单独的类，它们均继承自同一个**抽象类** `ASTNode`，该类提供了一个 `getName` 的抽象方法，用于输出题目要求输出的内容。

抽象类并没有提供其他功能，仅作为一个**组织作用**。

`@.frontend.ast.CompileUnit` 类组织了全部的语法单元，并暴露了一些接口给 `@.frontend.Parser`，供其进行相关内容的填充。

符号表相关内容位于 `@.frontend.symbol` 中，其中提供了符号项的基类 `Symbol`，包括符号的名字、类型、索引编号等，以及全局、不可变等属性。

符号类派生出了变量（`@.frontend.symbol.SymbolVariable`）与函数（`@.frontend.SymbolFunction`）两种。前者附有**潜在的静态初值**，后者附有**参数列表**。

#### 方法实现

语法分析采用带有回溯与超前查看的递归下降分析法，通过 Token 流中提供的一系列方法，进行超前查看、移进等。

下列代码展示了语法分析的入口方法，`among` 方法提供了超前查看并进行指定 token 类型的判断。

```java
while (!eof()) {
    if (among(1, TokenType.SpMain)) {
        // Main Function
        ast.setMainFunc(parseMainFuncDef());
    } else if (among(2, TokenType.LParen)) {
        // Function
        ast.addFunction(parseFuncDef());
    } else {
        // Declaration
        ast.addDeclaration(parseDeclaration());
    }
}
```

### 编码完成之后的修改

修改主要围绕课程需求进行，以及前期设计的不足而进行。

#### 架构变动

首先是增加了用于输出解析过程字符串的类与方法，为了集中统一管理且避免对结构造成过多的破坏，我增加了一个辅助类 `@.helpers.ParserWatcher` 进行。通过解析类中的 `watch` 方法，进行**规约时的记录**。

```java
private<T> T watch(T inst) {
    __watcher.add(inst.toString());
    return inst;
}
```

例如，下列代码中，使用这个方法对返回语句进行了记录，对整体解析器的结构几乎没有影响，唯一存在的问题就是遗漏。

```java
if (among(TokenType.Return)) {
    Token tk = consume(TokenType.Return);
    if (FirstSet.getFirst(Expr.class).contains(peekType())) {
        Expr expr = parseExpr();
        consume(TokenType.Semicolon);
        return watch(new StmtReturn(expr, tk));
    }
    consume(TokenType.Semicolon);
    return watch(new StmtReturn(tk));
}
```

因为需要考虑进行错误处理，而**回溯**是一个比较好的策略，在回溯时，需要**保存三样东西**：错误表、`ParserWatcher` 的记录、token 流的移进状态。我设计了 `@.utils.BackTrace` 类以及其内部接口 `Traceable` 进行。`BackTrace` 类中记录了**需要回溯的对象**，并提供方便的记录与恢复接口。而 `Traceable` 接口提供的接口也非常简单，只需要实现它们，就可以实现全自动的回溯！

```java
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
```

比如在判断是否是赋值语句时，采用了先试探 `LVal`，并检查是否有一个赋值号的方法进行，代码如下：

```java
try (BackTrace ignore = trace.save()) {
    parseLVal();
    if (!among(TokenType.Assign)) {
        throw new IllegalStateException("Not an assignment");
    }
} catch (Exception e) {
    return false;
}
```

一旦试探过程中产生错误，则会返回 `false`，表示不是一个赋值语句。无论试探结果如何，都会**自动回溯**，这虽然会略微影响性能，但对程序结构进行了极大的化简。

#### 调试与 Debug

主要是一些情况的遗漏、笔误等等。主要是错误处理的一些 corner case，导致先前架构的无力处理，可以通过回溯解决。其余错误没有留下过于深刻的影响，不再赘述。

## 中间代码生成

### 编码前的设计

#### 整体的代码组织结构

中间代码生成的相关内容位于 `@.llvm` 中，整体思路为使用其中的 `Traverser` 类的相关方法，对语法树进行遍历，并逐一生成。

在构建基本块时，引入了“**插入点**”的概念，方便了在构建分支等指令时的操作，同时也使得插入指令的逻辑更加清晰。

在构建基本块时，同时引入 `@.helpers.Comments` 类，为基本块维护一些注释，会包含一些语法树、代码行数等信息，例如：

```llvm
  21:			; For Cond Block @@5:5-5:7
    ; == snipped ==
    br i1 %24, label %25, label %55

  25:			; For Body Block @@5:5-5:7
    ; == snipped ==
    br i1 %31, label %32, label %51
```

#### 数据结构

对于 LLVM 来说，万物皆为 `Value`，基本块、指令、操作数都可以成为 `Value`，并可进一步细分为 `Constant`，`User`，`BasicBlock`，`Fucntion`，`GlobalVariable` 等。

`User` 一般用作指令（这个名字真的很怪，很讨厌），具备与其他 `Value` 的 `Use` 关系。每一种指令在创建与修改时，均会通过其操作数维护这样一种**使用**关系。

`User` 类中提供了众多的方法，对操作数进行操作，同时维护其 `use-def` 信息，例如：

```java
public void dropOperand(int index) {
    Value old = operands.get(index);
    operands.remove(index);
    this.removeSingleUse(old);
}
```

`Function` 类中通过自定义实现的双向链表，维护了函数体中的每一个基本块，以及函数的**形参**，它们也是 `Value` 类型。

`BasicBlock` 块通过双向链表维护了每一个指令，并能够提供常见的对指令的维护与判断操作。

整体 LLVM 按照下面的层次结构组织：

```
IrModule
  |
  |- GlobalVariable
  |
  `- Function
      |
      |- Value (params)
      |
      `- BasicBlock
          |
          `- User (instructions)
```

同时，每一层均通过重写 `toString()` 方法生成 LLVM IR 代码，其中会下降地调用下层结构。事实上，使用 `toString` 方法并不是一个很有趣的想法，因为调试的时候可能会比较麻烦。

> `DualLinkedList` **双向链表**的设计，提供了方便的遍历手段，以及向**任意一个节点**的前面或者后面插入内容，或者**删除任一节点的**的手段，以**内部类** `Node<T>` 为核心类，聚合于 `@.utils.DualLinkedList<T>` 中，非常方便好用，极大地辅助了后续代码优化的相关工作。

#### 方法实现

翻译过程整体上按照语法树进行，并逐一访问、生成各语法树成分。以表达式 `Stmt` 的翻译过程为例：

```java
void visitStmt(Stmt stmt) {
    switch (stmt.getType()) {
        case Break -> {
            localLoops.lastElement().breaks().add((IBranch) builder.insertInstruction(
                new IBranch(null)
            ));
            builder.createBlock(false, "Dummy Block for Break");
        }
        case Block -> visitBlock(((StmtBlock) stmt).getBlock());
        case GetInt -> performAssign(((StmtGetInt) stmt).getLeft(), builder.insertInstruction(
            new ICall(builder.getExternalFunction("getint"), List.of())
        ));
        case Expr -> Optional.ofNullable(((StmtExpr) stmt).getExpr()).ifPresent(this::visitExpr);
        case /* snipped */
    }
}
```

当然，类中提供了一些辅助方法，比如 `performAssign`，就具备了处理统一处理赋值语句的能力，并生成相应的 `gep` 以及 `store` 指令：

```java
void performAssign(LeftValue left, Value right) {
    Value pointer;
    if (left.getIndices().isEmpty()) {
        pointer = left.getSymbol().getValue();
    } else {
        pointer = builder.getGep(
            left.getSymbol(), left.getIndices().stream().map(this::visitExpr).toList()
        );
    }
    Value value = Compatibility.unityIntoPointer(pointer, right)[0];
    builder.insertInstruction(
        new IStore(value, pointer)
    );
}
```

在处理 `break` 与 `continue` 时，所插入的指令会跳转到 `null` 中，因为我执意维护块的逻辑顺序，所以暂时还无法生成目标块，因此采用 “**拉链——回填**” 技术，后续处理完整个循环后，会回过头去检视各个跳转语句，并填入正确的目标块。

在生成具体的 LLVM 指令时，我并没有将类似于 `getint` 特殊处理，它们仍然像普通函数那样被调用。对于 `printf` 等打印字符串的方法，我并没有使用 LLVM 的字符串类型，而是简单处理为逐字符打印。

### 编码完成之后的修改

#### 架构变动

在具体实现时，我错误地估计了类型转换的重要性与复杂性，最终，我添加了 `@.helpers.Compatibility` 这一个辅助类，通过插入相关的转换语句，进行指定类型的统一：

```java
public static Value[] unityIntoInteger(SymbolType target, Value... values) {
    return Stream.of(values).map(value -> {
        if (value.getType().equals(target)) {
            return value;
        } else {
            return builder.apply(new IConvert(target, value));
        }
    }).toArray(Value[]::new);
}
```

上述代码可以将 `values` 全部统一为 `target` 类型，并在需要的时候通过 `builder` 方法插入相关语句，并返回统一后的变量。`builder` 其实为 `IrBuilder::insertInstruction` 方法，会在构造时进行赋值。

```java
switch (expr.getOps().get(i)) {
    case ADD -> now = builder.insertInstruction(new IBinary(
      BinaryOp.ADD, Compatibility.unityIntoInteger(now, right)
    ));
    case SUB -> now = builder.insertInstruction(new IBinary(
      BinaryOp.SUB, Compatibility.unityIntoInteger(now, right)
    ));
}
```

这种调整，尽可能地维护了原有代码的逻辑，并且充分完成了其使命。

#### 调试与 Debug

在测试时，我发现在翻译 `printf` 语句时，因为对于**迭代器的误用**，导致参数调用顺序的问题，例如：

```c
int foo() {
    printf("FFF");
    return 1;
}

int main() {
    printf("AAA%dBBB", foo());
    return 0;
}
```

我会输出为 `AAADDD1BBB`，无论从什么角度看，这种输出方式都是极其异端的。核心的错误源于：

```java
Iterator<Value> args = ((StmtPrintf) stmt).getArgs().stream().map(this::visitExpr).iterator();
```

流生成的迭代器，只有在取用时才会执行 `map` 中注册的方法，也才会翻译为 LLVM 语句，而我正是在遍历字符串，碰到 `%d` 等时，才会取用迭代器的内容，遂导致了这个错误。解决方法是先转 `List`，让迭代器先走一遍，后续逐个取用即可。

此外遇到的问题便是控制流过于复杂，导致生成的基本块缺少跳转语句，常见于相遇的 `}`，例如：

```c
if (1) {
  if (2) {
    // == snipped ==
  }
  // 这里没有语句，生成时会忘记加跳转语句等等
}
```

解决方法是，仔细考虑生成逻辑，递归生成时多多关照其中的逻辑，将情况考虑全面。（~~说了等于没说~~）

## 代码优化

详见优化文章。

### 编码前的设计

#### 整体的代码组织结构

这一部分的代码优化主要集中于对于 LLVM IR 的分析（**analysises**）与变形（**transform**），因此我在 `@.llvm` 中又新建了这样的两个软件包，它们均需要继承 `@.llvm.Pass` 类，才能成为一个 Pass. 抽象类中有如下内容：

```java
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
```

核心为 `run` 方法，目前没有细分是针对 `IrModule` 或者 `Function` 或者 `BasicBlock`，全部统一传递 `IrModule` 对象。`debug` 方法实际应为 `final`，用于 pass 中的调试输出，受外部统一控制。`canPrintVerbose` 用于标记是否需要显示详细信息（即每执行一个 pass，就将得到的全新的 LLVM IR 输出到文件）。

在 `IrModule` 中，设计了一个 `runPass` 方法，可以快捷地执行不同的 pass，并根据配置输出相应的内容。执行的方法为：

```java
module
  .runPass(new Dummy())
  .runPass(new AnalyseControlFlowGraph())
  .runPass(new DeadBlockEliminate())
  .runPass(new AnalyseDominatorTree())
  . // == snipped ==
```

#### 数据结构

主要涉及的数据结构为 `ControlFlowGraph`，`DominatorTree` 与 `LoopInformation`，它们均位于 `@.llvm.data_structure` 中。它们分别提供了函数的**控制流图**与**支配树**模型，以及**单个循环**的相关属性与信息。

#### 方法实现

通过 `@.llvm.analysises.Analyse*` 相关 pass，可以解析函数等的信息，并填入相关数据结构，供后续优化 pass 使用。

### 编码完成之后的修改

#### 架构变动

在进行代码优化时，这些数据结构可能会遭到破坏，这就要求能够方便、准确地进行维护。`Optional` 并不是一个良好的选择，因此我另外实现了一个类 `@.utils.Container<T>`，进行相关对象的存储。常用的为 `set()`，`get()`，与 `invalidate()` 方法，分别设置、获取对象，以及标记为不可用。

在函数中，实际使用如下，这是为数不多的 `public` 成员对象。

```java
public final Container<ControlFlowGraph> cfg = new Container<>();
public final Container<DominatorTree> dom = new Container<>();
public final Container<HashMap<BasicBlock, LoopInformation>> loopMap = new Container<>();
public final Container<List<LoopInformation>> loops = new Container<>();
public final Container<List<LoopInformation>> allLoops = new Container<>();
```

#### 调试与 Debug

主要涉及代码优化的验证。在调试时，通过针对性的样例进行有效性检测，并通过测试库进行正确性的检测。

## 目标代码生成

首先，采用**虚拟寄存器**形式的后端代码表示，这种设计简化了 LLVM 向 Mips 的翻译，也使得后续工作（比如寄存器分配、Phi 指令移除）的目标更加纯粹。同时，Mips 相关体系结构也有**独立的结构层次**，这种设计更加清晰，但是使代码更加冗长。

针对寄存器翻译前后的两种表示，均预留了优化接口，但实际并未使用很多。

### 编码前的设计

#### 整体的代码组织结构

Mips 相关内容位于 `@.arch.mips` 中。`@.arch.ArchModule` 接口的意义是想做**多体系结构的兼容**，但从目前的设计来看比较失败，比如寄存器分配、Phi 指令移除等内容，都与 Mips 体系结构高度耦合，而没有进行进一步的抽象。

同 LLVM 一样，Mips 相关内容有着 `module -- function -- block -- instruction` 的体系，并通过 `toMips` 方法输出。

Mips 的操作数共有五种类型：基本块、函数、**立即数**、**物理寄存器**、**虚拟寄存器**，在生成 Mips 指令时，1️ 以立即数与虚拟寄存器为主，而后进行 Phi 指令移除，并将虚拟寄存器分配到物理寄存器之中。

在生成 Mips 指令时，我也设计了很多辅助方法，诸如 `buildBinaryHelperChooseI` 用于判断并快速选择是否使用带有 `i` 的指令；`buildMultDivHelper` 用于构建合法的乘除法指令，及其配套的 `mf*` 指令。

为了避免栈中**偏移量的回填或者修改**，我在处理函数调用时，对**栈约定**进行了调整，如下所示。主要变动是将参数放在靠近调用者的位置，这样即使不知道被调用者的栈大小，也可以顺利填入相关参数。同时，也动用了 `$fp` 寄存器，初步体会到了其存在的意义。

```
|                                        |
|           Caller Stack Frame           |
|                                        |
+========================================+  <-- Caller $sp == Callee $fp
|              Caller's $fp              |
+----------------------------------------+  <-- Caller $sp - 4
|            The 5th Argument            |
+----------------------------------------+  <-- Caller $sp - 8
|            The 6th Argument            |
+----------------------------------------+  <-- Caller $sp - 12
|             Other Argument             |
+----------------------------------------+
|             Local Variable             |
+----------------------------------------+
|           Reserved register            |
+----------------------------------------+  <-- Callee $sp
```

#### 数据结构

全部的 Mips 指令位于 `@.arch.mips.instruction` 中，继承自其中的 `MipsInstruction` 类，维护了虚拟寄存器的 use 与 def 关系，以及进行操作数替换的相关方法。

同 LLVM 不同的是，`MipsBlock` 中维护了块之间的**前驱后继关系**，并且在进行指令翻译时，维护 LLVM 块到 Mips 块的映射关系。

#### 方法实现

因为虚拟寄存器的存在，在进行指令翻译时，可以专心进行“翻译”这一件事情，因此这一部分的实现并不复杂，问题在于如何“优雅”地实现。因为 Mips（或者说是 Mars）的限制，很多时候需要进行特判，我在这些方法做得不够好。

针对一些预定义的**外部函数**，应该将它们转化为 Mars 的 `syscall` 指令，这也是在其中进行的特殊判断。

### 编码完成之后的修改

#### 架构变动

架构没有发生较大变动，只是 `@.arch.mips.process.MipsInstructionHub` 类的设计过于失败，后面逐渐弃用其中的方法。其根源在于，我在一开始**计划不使用伪指令**，所以对很多指令的生成进行了特殊判断，以达成直接生成基础指令的目的。但后续发现，这种执念没有意义，并且不利于进一步优化，便放弃了这种举措，带来了一定的架构变化。

此外，`@.arch.mips.process.MipsGenerator` 类承载了过多的内容，一些内容，比如构建具体指令的辅助方法，实际上应该放入 `MipsInstructionHub` 中，这也是架构设计的一些不足之处。

同时，在对于**函数运行栈大小**的维护与确定上，还存在着众多的问题，修改点过于分散，存在很多对于特殊情况的特殊处理导致可维护性较差，且存在较多的风险代码。

在后续进行优化时，我添加了对除常数的相关优化，因为时间有限，遂直接在翻译指令时进行，造成了架构的变动与愈发混乱。

#### 调试与 Debug

主要涉及 Phi 指令移除与寄存器分配的相关调试，以及对栈的维护上，前者相见优化文章，后者主要是搞清楚各个寄存器的含义与在函数调用前后的变化与恢复。

一个比较突出的问题在于，忽视了对于 `$ra` 寄存器的维护，以及对于 `$fp` 寄存器的保存上，在意识到相关问题后，也比较容易解决。
