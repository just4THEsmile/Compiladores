package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;

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
    public static Type getExprType(JmmNode expr, SymbolTable table, String method_name) {

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr,table, method_name);
            case VAR_REF_EXPR -> getVarExprType(expr, table, method_name);
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            case TYPE_ARRAY -> new Type(expr.get("name"), true);
            case NOT_EXPR -> Not_op(expr, table, method_name);
            case PAREN_EXPR -> getExprType(expr.getChildren().get(0), table, method_name);
            case MEMBER_CALL_EXPR -> get_member_call_expr(expr, table, method_name);

            case LENGTH_EXPR -> {
                var t = getExprType(expr.getChildren().get(0), table, method_name);
                if(t.isArray()){
                    yield new Type("int", false);
                }else{
                    yield new Type(null, false);
                }
            }
            case ARRAY_ACCESS_EXPR -> {
                var t = getExprType(expr.getChildren().get(0), table, method_name);
                var t2 = getExprType(expr.getChildren().get(1), table, method_name);
                if((t.isArray() || check_for_imports_type(t,table)) && ((t2.getName().equals("int") && !t2.isArray()) || check_for_imports_type(t2,table) )){
                        if (t.getName()=="varargs"){
                            yield new Type("int", false);
                        }else{
                            yield new Type(t.getName(), false);
                        }
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
                    t = getExprType(expr.getChildren().get(0), table, method_name);
                }
                for(var c : expr.getChildren()){
                    if(!getExprType(c, table, method_name).getName().equals(t.getName()) && !getExprType(c, table, method_name).isArray() && !check_for_imports_type(getExprType(c, table,method_name),table)){
                        yield new Type(null, false);
                    }
                }
                yield new Type(t.getName(),true);
            }
            case NEW_INT_ARRAY -> {
                var t = getExprType(expr.getChildren().get(0), table, method_name);
                if((t.getName().equals("int") || check_for_imports_type(t,table)) && !t.isArray()){
                    yield new Type("int", true);
                }
                yield new Type(null, true);
            }
            case NEW_OBJECT -> {
                var t = expr.get("classname");
                if(table.getClassName().contains(t)){
                    yield new Type(t, false);
                }
                yield new Type(null, false);
            }
            case METHOD_CALL_EXPR -> {
                var method = expr.get("name");
                for(var m : table.getMethods()){
                    if(m.equals(method)){
                        yield table.getReturnType(m);
                    }
                }
                yield new Type(null, false);
            }
            case THIS_REF_EXPR -> new Type(table.getClassName(), false);





            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getBinExprType(JmmNode binaryExpr, SymbolTable table, String method_name) {

        String operator = binaryExpr.get("op");
        switch (operator) {
            case "+", "*", "/","-" :
                if ( (getExprType(binaryExpr.getChildren().get(0), table, method_name).getName().equals(INT_TYPE_NAME)   &&
                        getExprType(binaryExpr.getChildren().get(1), table, method_name ).getName().equals(INT_TYPE_NAME) &&
                        !getExprType(binaryExpr.getChildren().get(0), table, method_name).isArray()                      &&
                        !getExprType(binaryExpr.getChildren().get(0), table, method_name).isArray()                  ) ||
                        check_for_imports_type(getExprType(binaryExpr.getChildren().get(0), table, method_name),table) ||
                        check_for_imports_type(getExprType(binaryExpr.getChildren().get(1), table, method_name),table)) {
                    return new Type(INT_TYPE_NAME, false);
                } else {
                    return new Type(null, false);
                }

            case "&&", "<" :
                if ( (getExprType(binaryExpr.getChildren().get(0), table, method_name).getName().equals("boolean") &&
                        !getExprType(binaryExpr.getChildren().get(0), table, method_name).isArray() &&
                        getExprType(binaryExpr.getChildren().get(1), table, method_name).getName().equals("boolean") &&
                        !getExprType(binaryExpr.getChildren().get(1), table, method_name).isArray() ) ||
                        check_for_imports_type(getExprType(binaryExpr.getChildren().get(0), table, method_name),table) ||
                        check_for_imports_type(getExprType(binaryExpr.getChildren().get(1), table, method_name),table)) {
                    return new Type("boolean", false);
                } else {
                    return new Type(null, false);
                }
            default :
                return new Type(null, false);
        }
    }


    public static Type getVarExprType(JmmNode varRefExpr, SymbolTable table, String method_name) {
        for (var symbol : table.getFields()) {
            if (symbol.getName().equals(varRefExpr.get("name"))) {
                return symbol.getType();
            }
        }
        for(var method : table.getMethods()){
            for(var param : table.getParameters(method)){
                if(param.getName().equals(varRefExpr.get("name")) && (method.equals(method_name) || method_name==null)){
                    return param.getType();
                }
            }
        }
        for (var method : table.getMethods()) {
            for (var local : table.getLocalVariables(method)) {
                if (local.getName().equals(varRefExpr.get("name")) && (method.equals(method_name) || method_name==null)) {
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
    public static boolean areTypesAssignable(Type sourceType, Type destinationType, SymbolTable table) {
        if(sourceType.isArray() && destinationType.isArray()){
            return (sourceType.getName().equals(destinationType.getName()) );
        } else if (!sourceType.isArray() && !destinationType.isArray()) {
            return ((sourceType.getName().equals(destinationType.getName()) || check_for_imports_type(sourceType, table) || check_for_imports_type(destinationType, table)));
        }else{
            return false;
        }
    }
    private static Type Not_op(JmmNode node, SymbolTable table, String method_name){
        var expr = node.getChildren().get(0);
        var type = getExprType(expr, table, method_name);
        if((type.getName().equals("boolean") && !type.isArray()) || check_for_imports_type(type, table)){
            return new Type("boolean", false);
        }else{
            return new Type(null, false);
        }
    }
    private static Type get_member_call_expr(JmmNode node, SymbolTable table, String method_name){
        var method = node.get("name");
        if (!node.getChildren().isEmpty()){
            Type t= getExprType(node.getJmmChild(0), table, method_name);
            if (t.getName()==null){
                return new Type(null, false);
            }
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

    public static Boolean check_for_imports_type(Type t1,SymbolTable table){
        List<String> imports = table.getImports();
        if (t1.getName() == null){
            return false;
        }
        for(String i : imports){
            if(i.contains(t1.getName())){
                return true;
            }
        }
        return false;
    }
}
