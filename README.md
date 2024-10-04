# BUAA-SysY-Compiler-2024 设计文档

> 22373080 沈锎

> 约定：在文档中，以 `@` 代替 Java 软件包 `top.swkfk.compiler` 或者路径 `top/swkfk/compiler`，比如 `@.frontend.Lexer` 代表类 `top.swkfk.compiler.frontend.Lexer`，`@/Configure.java` 代表文件 `top/swkfk/compiler/Configure.java`，这是为了简化文档内容，并参考了 js 的相关约定。
>
> <https://swkfk.top> 是我的博客主页，遂有上述软件包名。

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
