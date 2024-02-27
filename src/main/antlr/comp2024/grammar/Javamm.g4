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
MAIN : 'main' ;
TRUE : 'true' ;
FALSE : 'false' ;
RETURN : 'return' ;
IMPORT : 'import' ;
WHILE : 'while' ;
IF : 'if' ;
ELSE : 'else' ;
COMMA : ',' ;
NEW : 'new' ;
BOOLEAN : 'booleann' ;
STRING : 'String' ;




INTEGER : [0-9]+ ;
ID : [a-zA-Z0-9_$]+ ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDecl)* classDecl EOF
    ;


importDecl
    : IMPORT value+=ID ('.' value+=ID)* SEMI #Import
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
    | type name=ID '[' ']' SEMI
    | type name=ID EQUALS expr SEMI
    ;

type
    : type '[' ']'    #ArrayType
    | name= INT       #VarType
    | name= STRING    #VarType
    | name= INT '...' #VarType
    | name= BOOLEAN   #VarType
    | name= ID        #VarType
    | name= VOID      #VarType


    ;

mainMethodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
     STATIC VOID MAIN LPAREN 'String' '['']' name=ID RPAREN LCURLY (varDecl | stmt)* RCURLY
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
    STATIC?
    type name=ID
    LPAREN param* RPAREN
    LCURLY (varDecl | stmt)* RCURLY
    ;

param
    : type name=ID (COMMA param)* #Params
    ;

stmt
    : expr EQUALS expr SEMI #AssignStmt //
    | RETURN expr SEMI #ReturnStmt
    | LCURLY stmt* RCURLY #BlockStmt //
    | expr SEMI #ExprStmt //
    | WHILE LPAREN expr RPAREN stmt #WhileStmt //
    | IF LPAREN expr RPAREN stmt (ELSE stmt)? #IfStmt //
    ;

expr
    : NOT expr #NotExpr //
    | expr op= (MUL | DIV) expr #BinaryExpr //
    | expr op= (ADD | SUB) expr #BinaryExpr //
    | value=INTEGER #IntegerLiteral //
    | name=ID #VarRefExpr //
    | LPAREN expr RPAREN #ParenExpr //
    | expr op=AND expr #BinaryExpr //
    | expr op=SMALLER expr #BinaryExpr //
    | expr '.' ID LPAREN (expr)? (COMMA expr)* RPAREN #MemberCallExpr //
    | expr '.' 'length' #LengthExpr //
    | value=INTEGER #Integer //
    | expr '['expr']' #ArrayAccessExpr //
    | value = TRUE #BooleanLiteral //
    | value = FALSE #BooleanLiteral //
    | '['(expr)? (COMMA expr)*']' #Array //
    | NEW INT '['expr']' #NewIntArray //
    | NEW ID '('')' #NewObject //


    ;



