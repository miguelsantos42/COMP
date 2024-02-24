grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMICOLUMN : ';' ;
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
INT_VECTOR : 'int' LBRACKET RBRACKET ;
INT_VECTOR2: 'int' DOT DOT DOT;
IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;
TRUE : 'true' ;
FALSE : 'false' ;
THIS : 'this' ;
NEW : 'new' ;


INTEGER : '0'|([1-9][0-9]*) ;
ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;


WS : [ \t\n\r\f]+ -> skip ;

program
    : classDecl EOF
    ;


classDecl
    : CLASS name=ID
        ('extend' name=ID)?
        LCURLY
        methodDecl*
        RCURLY
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN param RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

param
    : type name=ID
    ;

// done
varDecl
    : type name=ID SEMICOLUMN
    ;

// done
type
    : name= INT
    | name= ID
    | name= BOOLEAN
    | name= INT_VECTOR
    | name= INT_VECTOR2
    ;

// done
stmt
    : ID EQUALS expr SEMICOLUMN #AssignStmt // a = 0;
    | RETURN expr SEMICOLUMN #ReturnStmt // return 0;
    | LCURLY stmt* RCURLY #BlockStmt // { a = 0; }
    | IF LPAREN expr RPAREN stmt (ELSE stmt)? #IfStmt // if (a) a = 0; else a = 1;
    | WHILE LPAREN expr RPAREN stmt #WhileStmt // while (a) a = 0;
    | ID LBRACKET expr RBRACKET EQUALS expr SEMICOLUMN #ArrayAssignStmt // a[0] = 0;
    | expr SEMICOLUMN #ExprStmt     // a.length; or a.method();
    ;

// done
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



