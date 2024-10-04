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

> TODO: 后续内容

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

> TODO: 后续内容

### 文件组织

参考编译器只有**单文件**，不存在文件组织的讨论。

## 编译器总体设计

我实现的编译器将采用 Java 语言编写，并采用面向对象程序的设计模式进行代码组织与编写。

### 总体结构

#### 数据结构

**Token** 与 **Token 流**，记录词法分析器产生的全部 Token，并提供方法供语法分析器采用。

**抽象语法树 AST** 与 **语法树成员**，全部的语法树成员，它们均需要继承 `@.frontend.ast.ASTNode` 这一个抽象类，并为它们提供了题目要求的输出方法。

**符号 symbol**，存放在符号表（`@.frontend.symbol.SymbolTable`）中的表项，包含原名、类型、mangle 名以及其他属性。

> mangle 这一词来自 Rust 的相关内容，表示对原有符号名进行唯一混淆得到的新名字，这一举措可以化解不同作用于的同名符号。

> TODO: 后续内容

#### 编译过程

我实现编译器清晰地分为下列过程：

- 词法分析（产生 `@.frontend.token.TokenStream` 供后续翻译过程使用）
- 语法分析（产生 `@.frontend.ast.CompileUnit`，存储全局常变量与函数，实际上是语法树）
- 语义分析（产生 `@.frontend.symbol.SymbolTable`，并将语法树中涉及到的符号与表项的引用绑定）
- 中间代码生成（/TODO/）
- 代码优化（/TODO/）
- 目标代码生成（/TODO/）

### 接口设计

在设计各个类时，我进行了较为严格的访问权限管理，除了偶尔疏忽，不暴露给外部的方法均不使用 `public`，同时，兼顾到了给子类使用的 `protected` 以及包内可见。

- 词法解析 `@.frontend.Lexer`：暴露构造方法、`lex()` 方法（进行词法解析）与 `emit()` 方法（返回 Token 流）
- 语法分析 `@.frontend.Parser`：暴露构造方法、`parse()` 方法（进行语法分析）与 `emit()` 方法（返回编译单元对象，语法树）
  - 语法分析需要提供一个 `@.frontend.token.TokenStream` 对象，通过其提供的非常丰富的方法访问 Token
  - 语法分析需要提供一个 `@.helpers.ParserWatcher` 对象，通过其 `add(String)` 方法进行作业要求的输出

> TODO: 后续内容

### 文件组织

- `Compiler.java`：编译器入口，调用方法解析参数，并调用相关过程。
- `@/Controller.java`：编译器的控制中心，承接编译过程中产生的对象，并根据配置选择执行内容。
- `@/Configure.java`，`@/HomeworkConfig.java`：对编译器的各项内容、调试、针对哪次作业进行配置。
- `@/helpers/`：存放与编译器有关的数据结构与算法内容。
- `@/utils/`：存放与编译器无关的数据结构。
- `@/error/`：对编译过程中产生的错误进行记录、输出的组件。
- `@/frontend/`：编译器前端内容：
  - `./ast/`：语法树及各级组件的定义
  - `./symbol/`：定义符号与符号表，以及各种奇怪的符号实体
  - `./token/`：定义 Token、类型、流等
  - `./Navigation.java`：提供记录元素在源代码中的位置信息
  - `./Lexer.java`：词法分析器，提取全部的 Token
  - `./Parser.java`：递归下降分析 Token，构建语法树
  - `./Traveser.java`：遍历语法树，构建符号表

> TODO: 后续内容

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
