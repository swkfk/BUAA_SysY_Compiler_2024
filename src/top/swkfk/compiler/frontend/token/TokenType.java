package top.swkfk.compiler.frontend.token;

@SuppressWarnings("SpellCheckingInspection")
public enum TokenType {

    Ident("IDENFR"),
    // ****** Literals ******
    IntConst("INTCON"),
    CharConst("CHRCON"),
    StrConst("STRCON"),
    // ****** Special symbols ******
    SpMain("MAINTK"),
    SpGetInt("GETINTTK"),
    SpGetChar("GETCHARTK"),
    SpPrintf("PRINTFTK"),
    // ****** Normal keep words ******
    Return("RETURNTK"),
    Const("CONSTTK"),
    Char("CHARTK"),
    Int("INTTK"),
    Void("VOIDTK"),
    For("FORTK"),
    Break("BREAKTK"),
    Continue("CONTINUETK"),
    If("IFTK"),
    Else("ELSETK"),
    // ****** Operators ******
    Not("NOT"),  // !
    And("AND"),  // &&
    Or("OR"),  // ||
    Plus("PLUS"),  // +
    Minus("MINU"),  // -
    Mult("MULT"),  // *
    Div("DIV"),  // /
    Mod("MOD"),  // %
    Lt("LSS"),  // <
    Leq("LEQ"),  // <=
    Gt("GRE"),  // >
    Geq("GEQ"),  // >=
    Eq("EQL"),  // ==
    Neq("NEQ"),  // !=
    Assign("ASSIGN"),  // =
    Semicolon("SEMICN"),  // ;
    Comma("COMMA"),  // ,
    LParen("LPARENT"),  // (
    RParen("RPARENT"),  // )
    LBrace("LBRACE"),  // {
    RBrace("RBRACE"),  // }
    LBracket("LBRACK"),  // [
    RBracket("RBRACK");  // ]

    private final String word;

    TokenType(String word) {
        this.word = word;
    }

    @Override
    public String toString() {
        return word;
    }
}
