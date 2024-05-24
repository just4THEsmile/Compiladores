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
    : CLASS name=( ID | 'main') ('extends' parent=ID)?
        LCURLY
        varDecl*
        methodDecl*
        mainMethodDecl?
        methodDecl*
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
    | name= (ID | 'main')
    ;

typeArray
    : name= INT
    | name= STRING
    | name= BOOLEAN
    | name= ID
    ;

mainMethodDecl locals[boolean isPublic=false, boolean isStatic=true]
    : (PUBLIC {$isPublic=true;})?
     STATIC VOID name='main' LPAREN ('String' '['']' arg=(ID|'main'|'length')) RPAREN LCURLY  varDecl* stmt* RCURLY
    ;

methodDecl locals[boolean isPublic=false , boolean isStatic=false]
    : (PUBLIC {$isPublic=true;})?
    (type|VOID) name=ID
    LPAREN paramlist? RPAREN
    LCURLY varDecl* stmt* RCURLY
    ;

paramlist
    : (param (COMMA param)* (COMMA INT '...' val=ID)?) #Params //
    | (INT '...' val=ID) #VarArgs //
    ;

param
    : type name=(ID | 'main' | 'length')
    ;

stmt
    : RETURN expr SEMI #ReturnStmt
    | LCURLY stmt* RCURLY #BlockStmt //
    | expr SEMI #ExprStmt //
    |expr EQUALS expr SEMI #AssignStmt //
    | WHILE LPAREN expr RPAREN stmt #WhileStmt //
    | IF LPAREN expr RPAREN stmt ELSE stmt #IfStmt //
    ;

expr
    : expr '.' 'length' #LengthExpr //
    | NOT expr #NotExpr //
    | 'this' #ThisRefExpr //
    | value = TRUE #BooleanLiteral //
    | name=(ID | 'main' |'length') #VarRefExpr //
    | value = FALSE #BooleanLiteral //
    | value=INTEGER #IntegerLiteral //
    | expr '['expr']' #ArrayAccessExpr //
    | '[' (expr (COMMA expr)*)? ']' #Array //
    | LPAREN expr RPAREN #ParenExpr //
    | expr ('.' name=ID LPAREN (expr (COMMA expr)*)? RPAREN) #MemberCallExpr //
    //| funcname=ID LPAREN (expr (COMMA expr)*)? RPAREN #MethodCallExpr //
    | expr op= (ADD | SUB) expr #BinaryExpr //
    | expr op= (MUL | DIV) expr #BinaryExpr //
    | expr op= SMALLER expr #BinaryExpr //
    | expr op= AND expr #BinaryExpr //
    | NEW name=(ID | 'main'| INT | BOOLEAN) '['expr']' #NewIntArray //
    | NEW classname=(ID | 'main') '('(expr (COMMA expr)*)?')' #NewObject //

    ;



