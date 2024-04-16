package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";

    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table) {

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr,table);
            case VAR_REF_EXPR -> getVarExprType(expr, table);
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            case TYPE_ARRAY -> new Type(expr.get("name"), true);
            case NOT_EXPR -> Not_op(expr, table);
            case PAREN_EXPR -> getExprType(expr.getChildren().get(0), table);
            case MEMBER_CALL_EXPR -> get_member_call_expr(expr, table);

            case LENGTH_EXPR -> {
                var t = getExprType(expr.getChildren().get(0), table);
                if(t.isArray()){
                    yield new Type("int", false);
                }else{
                    yield new Type(null, false);
                }
            }
            case ARRAY_ACCESS_EXPR -> {
                var t = getExprType(expr.getChildren().get(0), table);
                var t2 = getExprType(expr.getChildren().get(1), table);
                if(t.isArray() && t2.getName().equals("int") && !t2.isArray()){
                        yield new Type(t.getName(), false);
                }
                yield new Type(null, false);
            }
            case BOOLEAN_LITERAL -> new Type("boolean", false);
            case ARRAY ->{
                Type t =  new Type(null, false);
                if (expr.getChildren().isEmpty()) {
                    t =  new Type("empty", true);
                    yield t;
                }else{
                    t = getExprType(expr.getChildren().get(0), table);
                }
                for(var c : expr.getChildren()){
                    if(!getExprType(c, table).getName().equals(t.getName()) && !getExprType(c, table).isArray()){
                        yield new Type(null, false);
                    }
                }
                yield new Type(t.getName(),true);
            }





            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getBinExprType(JmmNode binaryExpr, SymbolTable table) {

        String operator = binaryExpr.get("op");
        switch (operator) {
            case "+", "*", "/","-" :
                if ( getExprType(binaryExpr.getChildren().get(0), table).getName().equals(INT_TYPE_NAME) &&
                        getExprType(binaryExpr.getChildren().get(1), table).getName().equals(INT_TYPE_NAME)) {
                    return new Type(INT_TYPE_NAME, false);
                } else {
                    return new Type(null, false);
                }

            case "&&", "<" :
                if ( getExprType(binaryExpr.getChildren().get(0), table).getName().equals("boolean") &&
                        getExprType(binaryExpr.getChildren().get(0), table).isArray() &&
                        getExprType(binaryExpr.getChildren().get(1), table).getName().equals("boolean") &&
                        getExprType(binaryExpr.getChildren().get(1), table).isArray() ) {
                    return new Type("boolean", false);
                } else {
                    return new Type(null, false);
                }
            default :
                return new Type(null, false);
        }
    }


    public static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
        for (var symbol : table.getFields()) {
            if (symbol.getName().equals(varRefExpr.get("name"))) {
                return symbol.getType();
            }
        }
        for(var method : table.getMethods()){
            for(var param : table.getParameters(method)){
                if(param.getName().equals(varRefExpr.get("name"))){
                    return param.getType();
                }
            }
        }
        for (var method : table.getMethods()) {
            for (var local : table.getLocalVariables(method)) {
                if (local.getName().equals(varRefExpr.get("name"))) {
                    return local.getType();
                }
            }

        }
        return new Type(null, false);
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        if(sourceType.isArray() && destinationType.isArray()){
            return sourceType.getName().equals(destinationType.getName());
        } else if (!sourceType.isArray() && !destinationType.isArray()) {
            return sourceType.getName().equals(destinationType.getName());
        }else{
            return false;
        }
    }
    private static Type Not_op(JmmNode node, SymbolTable table){
        var expr = node.getChildren().get(0);
        var type = getExprType(expr, table);
        if(type.getName().equals("boolean") && !type.isArray()){
            return new Type("boolean", false);
        }else{
            return new Type(null, false);
        }
    }
    private static Type get_member_call_expr(JmmNode node, SymbolTable table){
        var method = node.get("name");
        if (node.getChildren().isEmpty()){
            Type t= getVarExprType(node.getJmmChild(0), table);
            if (!t.isArray() && !t.getName().equals("int") && !t.getName().equals("boolean")) {
                if (t.getName().equals(table.getClassName())) {
                    for(var m : table.getMethods()){
                        if(m.equals(method)){
                            return table.getReturnType(m);
                        }
                    }
                    return new Type(null, false);
                }
                return new Type(t.getName(), false);
            }
        }
        return new Type(null, false);
    }
}
