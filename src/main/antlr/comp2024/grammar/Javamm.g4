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
        (mainMethodDecl | methodDecl)*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    | type name='main' SEMI
    | type name='length' SEMI
    | type name='string' SEMI
    | type name='String' SEMI
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

mainMethodDecl locals[boolean isPublic=false, boolean isStatic=true]
    : (PUBLIC {$isPublic=true;})?
     STATIC VOID name='main' LPAREN ('String' '['']' arg=ID)? RPAREN LCURLY  varDecl* stmt* RCURLY
    ;

methodDecl locals[boolean isPublic=false , boolean isStatic=false]
    : (PUBLIC {$isPublic=true;})?
    (STATIC{$isStatic=true;})?
    (type|VOID) name=ID
    LPAREN paramlist? RPAREN
    LCURLY varDecl* stmt* RCURLY
    ;

paramlist
    : (param (COMMA param)* (COMMA INT '...' val=ID)?) #Params //
    | (INT '...' val=ID) #VarArgs //
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
    | expr ('.' name=ID LPAREN (expr (COMMA expr)*)? RPAREN) #MemberCallExpr //
    | funcname=ID LPAREN (expr (COMMA expr)*)? RPAREN #MethodCallExpr //
    | expr op= (MUL | DIV) expr #BinaryExpr //
    | expr op= (ADD | SUB) expr #BinaryExpr //
    | expr op= (AND|SMALLER) expr #BinaryExpr //
    | value=INTEGER #IntegerLiteral //
    | name=ID #VarRefExpr //
    | LPAREN expr RPAREN #ParenExpr //
    | expr '.' 'length' #LengthExpr //
    | expr '['expr']' #ArrayAccessExpr //
    | value = TRUE #BooleanLiteral //
    | value = FALSE #BooleanLiteral //
    | NEW INT '['expr']' #NewIntArray //
    | NEW classname=ID '('(expr (COMMA expr)*)?')' #NewObject //

    | '[' (expr (COMMA expr)*)? ']' #Array //
    | 'this' #ThisRefExpr //
    ;



