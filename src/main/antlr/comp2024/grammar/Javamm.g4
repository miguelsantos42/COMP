grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMICOLON : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
LBRACKET : '[' ;
RBRACKET : ']' ;
EXCLAMATION : '!' ;
DOT : '.' ;
COMMA : ',' ;
MUL : '*' ;
DIV : '/' ;
ADD : '+' ;
SUB : '-' ;
AND : '&&' ;
LT : '<' ;

CLASS : 'class' ;
INT : 'int' ;
PUBLIC : 'public' ;
RETURN : 'return' ;
BOOLEAN : 'boolean' ;
INT_VECTOR : 'int' (' ')? LBRACKET (' ')? RBRACKET ;
INT_VECTOR2: 'int' (' ')? DOT DOT DOT;
IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;
TRUE : 'true' ;
FALSE : 'false' ;
THIS : 'this' ;
NEW : 'new' ;
STRING : 'String';

MAIN_LINE : 'static void main';

INTEGER : '0'|([1-9][0-9]*) ;
ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;

LINE_COMMENT : '//' ~[\r\n]* -> skip;
BLOCK_COMMENT : '/*' .*? '*/' -> skip;

WS : [ \t\n\r\f]+ -> skip ;

program
    : importDecl* classDecl EOF
    ;

importDecl
    : 'import' name += ID ( '.' name += ID )* ';' #ImportDeclaration
    /*
        import java.util.Map;
    */
    ;


classDecl
    : CLASS name=ID ('extends' extendedName=ID)?
        LCURLY
        (classBody)
        RCURLY #ClassDeclaration
    /*
        class A extends B {
            // body
        }
    */
    ;

classBody
    : varDecl* (methodDecl | mainMethodDecl)* #ClassBodyDeclaration
    /*
        int a;
        public int a(int b){
            int a;
            a = 0;
        }
    */
    ;

methodDecl
    : (PUBLIC)?
        type name=ID
        LPAREN (param)? RPAREN
        block #MethodDeclaration
    /*
    public int a(int b){
        int a;
        a = 0;
    }
    */
    ;

mainMethodDecl
    : (PUBLIC)? MAIN_LINE LPAREN STRING LBRACKET RBRACKET ID RPAREN mainBlock #MainMethodDeclaration
    /*
    public static void main(String[] args){
        int a;
        a = 0;
    }
    */
    ;

block
    : LCURLY varDecl* stmt* returnStatement RCURLY #MethodCodeBlock // { int a; a = 0; }
    ;

mainBlock
    :LCURLY varDecl* stmt* RCURLY #MethodMainCodeBlock // { int a; a = 0; }
    ;

returnStatement
    : RETURN expr SEMICOLON #ReturnStmt // return 0;
    ;

param
    : type name += ID (COMMA type name += ID)* #FunctionParameters // int a, int b
    ;

varDecl
    : type name=ID SEMICOLON #VarDeclaration // int a;
    ;

type
    : name= INT #IntType // int
    | name= ID #IDType // example: class instance
    | name= BOOLEAN #BoolType // boolean
    | name= INT_VECTOR #IntVectorType1 // int[]
    | name= INT_VECTOR2 #IntVectorType2 // int...
    | name= STRING #StringType // String
    ;

stmt
    : ID EQUALS expr SEMICOLON #AssignStmt // a = 0;
    | LCURLY stmt* RCURLY #BlockStmt // { a = 0; }
    | IF LPAREN expr RPAREN stmt ELSE stmt   #IfStmt // if (a) a = 0; else a = 1;
    | WHILE LPAREN expr RPAREN stmt #WhileStmt // while (a) a = 0;
    | ID LBRACKET expr RBRACKET EQUALS expr SEMICOLON #ArrayAssignStmt // a[0] = 0;
    | expr SEMICOLON #ExprStmt     // a.length; or a.method();
    ;

expr
    : LPAREN expr RPAREN #ParenthesisExpr // (a)
    | expr LBRACKET expr RBRACKET #ArrayAccessExpr // a[b]
    | expr DOT 'length' #ArrayLengthExpr // a.length
    | expr DOT name=ID LPAREN (expr (COMMA expr)*)? RPAREN #MethodCallExpr // a.method(b, c)
    | EXCLAMATION expr #NegationExpr // !a
    | NEW INT LBRACKET expr RBRACKET #NewArrayExpr // new int[a]
    | NEW ID LPAREN RPAREN #NewObjectExpr // new A()
    | expr op= MUL expr #BinaryExpr // a * b
    | expr op= DIV expr #BinaryExpr // a / b
    | expr op= ADD expr #BinaryExpr // a + b
    | expr op= SUB expr #BinaryExpr // a - b
    | expr op= LT expr #LogicalExpr // a < b
    | expr op= AND expr #LogicalExpr // a && b
    | LBRACKET ( expr (COMMA expr)* )? RBRACKET #ArrayAccessExpr // [a, b, c]
    | value=INTEGER #IntegerLiteral // 0
    | name=ID #VarRefExpr // a
    | name=THIS #ThisExpr // this
    | name=TRUE #BooleanLiteral // true
    | name=FALSE #BooleanLiteral // false
    ;



