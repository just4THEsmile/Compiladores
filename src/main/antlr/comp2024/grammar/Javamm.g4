grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}


ENDCOMMENT : '*/' ;
LINECOMMENT : '//' .*? '\n'-> skip ;
COMMENT     : '/*' .*? '*/'-> skip ;



EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
MUL : '*' ;
ADD : '+' ;
SUB : '-' ;
DIV : '/' ;
NOT : '!' ;
AND : '&&' ;
SMALLER : '<' ;



CLASS : 'class' ;
INT : 'int' ;
PUBLIC : 'public' ;
STATIC : 'static' ;
VOID : 'void' ;
TRUE : 'true' ;
FALSE : 'false' ;
RETURN : 'return' ;
IMPORT : 'import' ;
WHILE : 'while' ;
IF : 'if' ;
ELSE : 'else' ;
COMMA : ',' ;
NEW : 'new' ;
BOOLEAN : 'boolean' ;
STRING : 'String' ;




INTEGER : [1-9][0-9]* |[0,9]  ;
ID : [a-zA-Z_$][a-zA-Z0-9_$]*;

WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDecl)* classDecl EOF
    ;


importDecl
    : IMPORT value+=ID ('.' value+=ID)* SEMI
    ;

classDecl
    : CLASS name=ID ('extends' parent=ID)?
        LCURLY
        varDecl*
        mainMethodDecl*
        methodDecl*
        mainMethodDecl*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    | type name='main' SEMI
    | type name='length' SEMI
    | type name='string' SEMI
    ;

type
    : typeArray '[' ']'
    | name= INT
    | name= STRING
    | name= BOOLEAN
    | name= ID
    ;

typeArray
    : name= INT
    | name= STRING
    | name= BOOLEAN
    | name= ID
    ;

mainMethodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
     STATIC VOID 'main' LPAREN ('String' '['']' arg=ID)? RPAREN LCURLY  varDecl* stmt* RCURLY
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
    STATIC?
    (type|VOID) name=ID
    LPAREN paramlist? RPAREN
    LCURLY varDecl* stmt* RCURLY
    ;

paramlist
    : (param (COMMA param)* (COMMA INT '...' val=ID)?) //
    | (INT '...' val=ID)
    ;

param
    : type name=ID
    ;

stmt
    : expr EQUALS expr SEMI #AssignStmt //
    | RETURN expr SEMI #ReturnStmt
    | LCURLY stmt* RCURLY #BlockStmt //
    | expr SEMI #ExprStmt //
    | WHILE LPAREN expr RPAREN stmt #WhileStmt //
    | IF LPAREN expr RPAREN stmt ELSE stmt #IfStmt //
    ;

expr
    : NOT expr #NotExpr //
    | expr op= (MUL | DIV) expr #BinaryExpr //
    | expr op= (ADD | SUB) expr #BinaryExpr //
    | expr op= (AND|SMALLER) expr #BinaryExpr //
    | value=INTEGER #IntegerLiteral //
    | name=ID #VarRefExpr //
    | LPAREN expr RPAREN #ParenExpr //
    | expr op=SMALLER expr #BinaryExpr //
    | expr ('.' name=ID LPAREN (expr (COMMA expr)*)? RPAREN) #MemberCallExpr //
    | expr '.' 'length' #LengthExpr //
    | value=INTEGER #Integer //
    | expr '['expr']' #ArrayAccessExpr //
    | value = TRUE #BooleanLiteral //
    | value = FALSE #BooleanLiteral //
    | '['(expr)? (COMMA expr)*']' #Array //
    | NEW INT '['expr']' #NewIntArray //
    | NEW classname=ID '('(expr (COMMA expr)*)?')' #NewObject //
    | funcname=ID LPAREN (expr (COMMA expr)*)? RPAREN #MethodCallExpr //
    | value=ID op=('++' | '--') #UnaryOp //
    | '[' (expr (COMMA expr)*)? ']' #Array //
    | 'this' #ThisRefExpr //
    ;



