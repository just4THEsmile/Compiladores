package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.*;
import java.util.stream.Collectors;

public class Analyser extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(Kind.METHOD_DECL, this::visitMethod);
        addVisit(Kind.ASSIGN_STMT, this::Check_Assign_STM);
        addVisit(Kind.PROGRAM, this::dealWithProgram);
        addVisit(Kind.TYPE, this::dealWithType);
        addVisit(Kind.VAR_REF_EXPR, this::Check_decl);
        addVisit(Kind.RETURN_STMT, this::Check_return);
        addVisit(Kind.ARRAY, this::dealWithArray);
        addVisit(Kind.MEMBER_CALL_EXPR, this::dealWithMemberCallExpr);
        addVisit(Kind.METHOD_CALL_EXPR, this::dealWithCallExpr);
        addVisit(Kind.IF_STMT, this::dealWithIf);
        addVisit(Kind.WHILE_STMT, this::dealWithWhile);
        addVisit(Kind.LENGTH_EXPR, this::dealWithLength);
        addVisit(Kind.THIS_REF_EXPR, this::dealWithThis);
        addVisit(Kind.PARAM, this::dealWithParam);
        addVisit("ImportDecl", this::dealWithImport);



    }

    private Void dealWithProgram(JmmNode node, SymbolTable table) {
        for (JmmNode child : node.getChildren()) {
            visit(child, table);
        }
        return null;
    }

    private Void dealWithImport(JmmNode node, SymbolTable table) {
        int s= node.get("value").lastIndexOf(",");

        String importName = node.get("value").substring(s+1).replace("[","").replace("]","").replace(" ","");
        boolean isImport=false;
        for (String import_ : table.getImports()) {
            if (import_.equals(importName)) {
                if(isImport){
                    addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                            "Import duplicated " + importName));
                    return null;
                }
                isImport=true;
            }
        }
        if( importName.equals(table.getClassName())){
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                    "Import name has the same name of class" + importName));
            return null;
        }
        return null;
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table) {
        String method = get_Caller_method(varDecl);
        boolean isField=false;
        boolean isParam=false;
        boolean isLocal=false;
        if(method==null){ // is field or
            // check form reapeated fields
            for (Symbol field : table.getFields()) {
                if (field.getName().equals(varDecl.get("name"))) {
                    if(isField){
                        addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(varDecl), NodeUtils.getColumn(varDecl),
                                "Field duplicated " + varDecl.get("name")));
                        return null;
                    }
                    isField=true;
                }
            }
            for (String method_ : table.getMethods()) {
                method = method_;

                for (Symbol local : table.getLocalVariables(method)) {
                    if (local.getName().equals(varDecl.get("name"))) {
                        if ( isLocal) {
                            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(varDecl), NodeUtils.getColumn(varDecl),
                                    "Variable duplicated " + varDecl.get("name")));
                            return null;
                        }
                        isLocal = true;
                    }
                }
                for (Symbol param : table.getParameters(method)) {
                    if (param.getName().equals(varDecl.get("name"))) {
                        if ((isLocal ) || (isParam)) {
                            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(varDecl), NodeUtils.getColumn(varDecl),
                                    "Variable duplicated " + varDecl.get("name")));
                            return null;
                        }
                        isParam = true;
                    }
                }
            }
            return null;
        }
        for (Symbol local : table.getLocalVariables(method) ){
            if (local.getName().equals(varDecl.get("name"))){
                if(isLocal){
                    addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(varDecl), NodeUtils.getColumn(varDecl),
                            "Variable duplicated " + varDecl.get("name")));
                    return null;
                }
                isLocal=true;
            }
        }
        for (Symbol param : table.getParameters(method) ){
            if (param.getName().equals(varDecl.get("name"))){
                if(isLocal  ||  isParam){
                    addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(varDecl), NodeUtils.getColumn(varDecl),
                            "Variable duplicated " + varDecl.get("name")));
                    return null;
                }
                isParam=true;
            }
        }


        return null;
    }

    private Void dealWithParam(JmmNode node, SymbolTable table) {
        String method = get_Caller_method(node);
        boolean isParam=false;
        for (Symbol local : table.getParameters(method) ){
            if (local.getName().equals(node.get("name"))){
                if(isParam){
                    addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                            "Variable duplicated " + node.get("name")));
                    return null;
                }
                isParam=true;
            }
        }
        return null;
    }


    private Void dealWithThis(JmmNode node, SymbolTable table) {
        String method = get_Caller_method(node);
        if (method.equals("main")) {
            int line = NodeUtils.getLine(node);
            int column = NodeUtils.getColumn(node);
            String message = "Found \"this\" in static main function";
            addReport(Report.newError(Stage.SEMANTIC, line, column, message, null));
        }
        return null;
    }

    private Void assignVisit(JmmNode node, SymbolTable table) {
        JmmNode typeNode = node.getChildren().get(0);
        JmmNode valueNode = node.getChildren().get(1);

        String assigneeType = typeNode.getKind().toString();
        String assignedType = valueNode.getKind().toString();

        Type assigneeSymbol = getTypeFromName(assigneeType, table);
        if (assigneeSymbol == null) {
            int line = typeNode.get("lineStart") != null ? Integer.parseInt(typeNode.get("lineStart")) : -1;
            int column = typeNode.get("colStart") != null ? Integer.parseInt(typeNode.get("colStart")) : -1;
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, line, column,
                    "Variable '" + assigneeType + "' is not declared"));
            return null;
        }

        Type assignedSymbol = getTypeFromName(assignedType, table);
        if (assignedSymbol == null) {
            int line = valueNode.get("lineStart") != null ? Integer.parseInt(valueNode.get("lineStart")) : -1;
            int column = valueNode.get("colStart") != null ? Integer.parseInt(valueNode.get("colStart")) : -1;
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, line, column,
                    "Variable '" + assignedType + "' is not declared"));
            return null;
        }

        if (!isSubtypeOf(assignedSymbol, assigneeSymbol, table)) {
            int line = node.get("lineStart") != null ? Integer.parseInt(node.get("lineStart")) : -1;
            int column = node.get("colStart") != null ? Integer.parseInt(node.get("colStart")) : -1;
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, line, column,
                    "Type of the assigned must be subtype of the assignee"));
        }

        return null;
    }

    private Type getTypeFromName(String typeName, SymbolTable table) {
        return table.getReturnType(typeName);
    }

    private boolean isPrimitiveType(Type type) {
        String typeName = type.getName();
        return typeName.equals("int") || typeName.equals("boolean") || typeName.equals("char") ||
                typeName.equals("byte") || typeName.equals("short") || typeName.equals("long") ||
                typeName.equals("float") || typeName.equals("double");
    }

    private Class<?> getClassFromName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
    private boolean isSubtypeOf(Type assigned, Type assignee, SymbolTable table) {
        // Check if types are equal
        if (assigned.equals(assignee)) {
            return true;
        }

        // Check if assigned type is a primitive type
        if (isPrimitiveType(assigned)) {
            return false;
        }

        // Check if assigned type is an array
        if (assigned.isArray() && assignee.isArray()) {
            return isSubtypeOf(new Type(assigned.getName(), false), new Type(assignee.getName(), false), table);
        }

        // Check if assigned type is a subclass of assignee
        String assignedName = assigned.getName();
        while (assignedName != null) {
            if (assignedName.equals(assignee.getName())) {
                return true;
            }
            Type superType = table.getReturnType(assignedName);
            if (superType == null) {
                break;
            }
            assignedName = superType.getName();
        }

        // Check if assigned type is a subclass of assignee by class inheritance
        Class<?> assignedClass = getClassFromName(assigned.getName());
        Class<?> assigneeClass = getClassFromName(assignee.getName());
        if (assignedClass != null && assigneeClass != null) {
            return assigneeClass.isAssignableFrom(assignedClass);
        }

        return false;
    }


    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        JmmNode leftOperand = binaryExpr.getChildren().get(0);
        JmmNode rightOperand = binaryExpr.getChildren().get(1);

        String method = get_Caller_method(binaryExpr);
        Type leftType = TypeUtils.getExprType(leftOperand, table,method);
        Type rightType = TypeUtils.getExprType(rightOperand, table,method);

        if ( leftType.getName().equals(rightType.getName()) &&
                (leftType.isArray() == rightType.isArray()) &&
                (leftType.getName()!=null)
        ){

        }else{
            int line = binaryExpr.get("lineStart") != null ? Integer.parseInt(binaryExpr.get("lineStart")) : -1;
            int column = binaryExpr.get("colStart") != null ? Integer.parseInt(binaryExpr.get("colStart")) : -1;
            String message = "Operands of relational operations must be of type int";
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, line, column, message));

        }
        return null;
    }


    private Void visitMethod(JmmNode methodNode, SymbolTable table) {
        String methodName = methodNode.get("name");
        String methodSignature = getMethodSignature(methodNode);

        // Verificar se o método está presente na tabela de símbolos
        if (!table.getMethods().contains(methodName)) {
            SymbolTable currentTable = table;
            boolean foundInSuperclass = false;
            /*
            // Procurar o método nas superclasses recursivamente
            while (currentTable != null && currentTable.getSuper() != null) {
                String superclass = currentTable.getSuper();
                currentTable = findClassSymbolTable(superclass, table);
                if (currentTable != null && currentTable.getMethods().contains(methodSignature)) {
                    foundInSuperclass = true;
                    break;
                }
            }*/

            int line = methodNode.get("lineStart") != null ? Integer.parseInt(methodNode.get("lineStart")) : -1;
            int column = methodNode.get("colStart") != null ? Integer.parseInt(methodNode.get("colStart")) : -1;
            String message = "Method '" + methodName + "' is not declared in the current class or any imported superclass";
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, line, column, message));
        }

        // Verificar tipos dos argumentos passados para o método
        List<JmmNode> paramNodes = methodNode.getChildren("PARAMS");
        List<Type> paramTypes = paramNodes.stream()
                .map(param -> getTypeFromName(param.get("type"), table))
                .collect(Collectors.toList());

        List<JmmNode> argNodes = methodNode.getChildren("ARGS");

        if (paramTypes.size() != argNodes.size()) {
            int line = methodNode.get("lineStart") != null ? Integer.parseInt(methodNode.get("lineStart")) : -1;
            int column = methodNode.get("colStart") != null ? Integer.parseInt(methodNode.get("colStart")) : -1;
            String message = "Number of arguments does not match method declaration";
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, line, column, message));
        } else {
            for (int i = 0; i < paramTypes.size(); i++) {
                Type paramType = paramTypes.get(i);
                JmmNode argNode = argNodes.get(i);
                Type argType = TypeUtils.getExprType(argNode, table,null);

                if (!isSubtypeOf(argType, paramType, table)) {
                    int line = argNode.get("lineStart") != null ? Integer.parseInt(argNode.get("lineStart")) : -1;
                    int column = argNode.get("colStart") != null ? Integer.parseInt(argNode.get("colStart")) : -1;
                    String message = "Argument type '" + argType.getName() + "' is not compatible with parameter type '" + paramType.getName() + "'";
                    addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, line, column, message));
                }
            }
        }

        // Verificar se o método estático está usando a palavra-chave "this"
        boolean isStatic = methodNode.get("isStatic") != null && methodNode.get("isStatic").equals("true");
        if (isStatic && containsThisExpression(methodNode)) {
            int line = methodNode.get("lineStart") != null ? Integer.parseInt(methodNode.get("lineStart")) : -1;
            int column = methodNode.get("colStart") != null ? Integer.parseInt(methodNode.get("colStart")) : -1;
            String message = "Static method cannot use 'this' expression";
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, line, column, message));
        }

        // Verificar se o método possui parâmetros varargs
        if (!paramNodes.isEmpty()) {
            JmmNode lastParamNode = paramNodes.get(paramNodes.size() - 1);
            boolean isVarArg = lastParamNode.get("vararg") != null && lastParamNode.get("vararg").equals("true");
            if (isVarArg) {
                // Verificar se o vararg é o último parâmetro e se não é o único parâmetro
                if (paramNodes.size() == 1) {
                    int line = lastParamNode.get("lineStart") != null ? Integer.parseInt(lastParamNode.get("lineStart")) : -1;
                    int column = lastParamNode.get("colStart") != null ? Integer.parseInt(lastParamNode.get("colStart")) : -1;
                    String message = "Vararg parameter cannot be the only parameter in the method declaration";
                    addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, line, column, message));
                }
            } else {
                // Verificar se não há parâmetros após o vararg
                for (int i = paramNodes.size() - 2; i >= 0; i--) {
                    JmmNode paramNode = paramNodes.get(i);
                    if (paramNode.get("vararg") != null && paramNode.get("vararg").equals("true")) {
                        int line = paramNode.get("lineStart") != null ? Integer.parseInt(paramNode.get("lineStart")) : -1;
                        int column = paramNode.get("colStart") != null ? Integer.parseInt(paramNode.get("colStart")) : -1;
                        String message = "Vararg parameter must be the last parameter in the method declaration";
                        addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, line, column, message));
                        break;
                    }
                }
            }
        }


        // Verificar se há varargs em declarações de variáveis, campos ou retornos de métodos
        checkVarArgsInMethod(methodNode, table);
        // Implementar verificação de tipos dos argumentos passados para o método

        return null;
    }

    private String getMethodSignature(JmmNode methodNode) {
        StringBuilder signatureBuilder = new StringBuilder();
        signatureBuilder.append(methodNode.get("name")).append("(");
        for (JmmNode param : methodNode.getChildren("PARAM")) {
            signatureBuilder.append(param.get("type")).append(",");
        }
        if (!methodNode.getChildren("PARAM").isEmpty()) {
            signatureBuilder.deleteCharAt(signatureBuilder.length() - 1);
        }
        signatureBuilder.append(")");
        return signatureBuilder.toString();
    }

    private SymbolTable findClassSymbolTable(String className, SymbolTable currentTable) {
        if (currentTable.getClassName().equals(className)) {
            return currentTable;
        }

        String superclass = currentTable.getSuper();
        if (superclass != null) {
            return findClassSymbolTable(className, findClassSymbolTable(superclass, currentTable));
        }

        return null;
    }


    private boolean containsThisExpression(JmmNode node) {
        if (node.getKind().equals("THIS")) {
            return true;
        }
        for (JmmNode child : node.getChildren()) {
            if (containsThisExpression(child)) {
                return true;
            }
        }
        return false;
    }

    private void checkVarArgsInMethod(JmmNode methodNode, SymbolTable table) {
        // Verificar se há varargs em declarações de variáveis, campos ou retornos de métodos
        List<JmmNode> varArgsNodes = new ArrayList<>();
        collectVarArgsNodes(methodNode, varArgsNodes);

        for (JmmNode varArgsNode : varArgsNodes) {
            int line = varArgsNode.get("lineStart") != null ? Integer.parseInt(varArgsNode.get("lineStart")) : -1;
            int column = varArgsNode.get("colStart") != null ? Integer.parseInt(varArgsNode.get("colStart")) : -1;
            String message = "Vararg parameter cannot be used in variable declarations, field declarations, or method return types";
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, line, column, message));
        }
    }

    private void collectVarArgsNodes(JmmNode node, List<JmmNode> varArgsNodes) {
        if (node == null) {
            return;
        }
        if (node.getKind().equals("_varargs")) {
            varArgsNodes.add(node);
        }
        for (JmmNode child : node.getChildren()) {
            collectVarArgsNodes(child, varArgsNodes);
        }
    }

    private Void Check_Assign_STM(JmmNode assign_stm, SymbolTable table) {
        String MethodName = get_Caller_method(assign_stm);


        Type exp1 = TypeUtils.getExprType(assign_stm.getChildren().get(0),table,MethodName);
        System.out.println((assign_stm.toTree()));
        Type exp2 = TypeUtils.getExprType(assign_stm.getChildren().get(1),table,MethodName);
        if (exp1.getName()==null || exp2.getName()==null){
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(assign_stm), NodeUtils.getColumn(assign_stm),
                    "Error variable not declared " + exp1.getName()));
        }
        if (table.getSuper()!= null){
            if ((Objects.equals(exp1.getName(), table.getClassName()) && Objects.equals(exp2.getName(), table.getSuper())) ||(Objects.equals(exp2.getName(), table.getClassName()) && Objects.equals(exp1.getName(), table.getSuper()))){
                return null;
            }
        }
        var testt=exp2.getName();
        var testtt= Objects.equals(exp2.getName(), table.getClassName());
        Boolean check= (Objects.equals(exp1.getName(), table.getClassName()) && !Objects.equals(exp2.getName(), exp1.getName()) && !TypeUtils.check_for_imports_derivs(exp2,table));
        Boolean check1= (Objects.equals(exp2.getName(), table.getClassName()) && !Objects.equals(exp1.getName(), exp2.getName()) && !TypeUtils.check_for_imports_derivs(exp1,table));
        if ((Objects.equals(exp1.getName(), table.getClassName()) && !Objects.equals(exp2.getName(), exp1.getName()) &&!TypeUtils.check_for_imports_derivs(exp2,table)) || (Objects.equals(exp2.getName(), table.getClassName()) && !Objects.equals(exp1.getName(), exp2.getName()) && !TypeUtils.check_for_imports_derivs(exp1,table))){
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(assign_stm), NodeUtils.getColumn(assign_stm),
                    "Error variable not declared " + exp1.getName()));

        }
        var test=TypeUtils.check_for_imports_type(exp1,table);
        var test1 =TypeUtils.check_for_imports_derivs(exp2,table);
        if (!exp1.equals(exp2) && !(TypeUtils.check_for_imports_type(exp1,table)) && !TypeUtils.check_for_imports_derivs(exp2,table)){
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(assign_stm), NodeUtils.getColumn(assign_stm),
                    "Types differ " + exp1.getName() + " and " + exp2.getName()));
        }






        return null;
    }

    private void checkWhileStatement(JmmNode expression, SymbolTable table) {
        String method = get_Caller_method(expression);
        Type exprType = TypeUtils.getExprType(expression, table, method);
        if (exprType.isArray() || !exprType.getName().equals("boolean")) {
            int line = expression.get("lineStart") != null ? Integer.parseInt(expression.get("lineStart")) : -1;
            int column = expression.get("colStart") != null ? Integer.parseInt(expression.get("colStart")) : -1;
            String message = "Invalid while condition";
            addReport(new Report(ReportType.WARNING, Stage.SEMANTIC, line, column, message));
        }
    }

    private Void dealWithType(JmmNode node, SymbolTable table) {
        if (node.getChildren().isEmpty()) {
            return null;
        }
        String method = get_Caller_method(node);
        Type arrayType = TypeUtils.getExprType(node.getChildren().get(0), table,method);
        //Type indexType = TypeUtils.getExprType(node.getChildren().get(1), table);

        if (arrayType == null) {
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                    "ArrayType is null"));
        }
        /*
        if (indexType == null) {
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                    "IndexType is null"));
        }
        */
        if (arrayType != null && !arrayType.isArray()) {
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                    "Array object must be an array but found " + arrayType.getName()));
        }
        /*
        if (indexType != null && !indexType.getName().equals(TypeUtils.getIntTypeName())) {
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                    "Array index expression is not Integer"));
        }*/
        return null;
    }

    private Void Check_decl(JmmNode node, SymbolTable table) {
        String method = get_Caller_method(node);
        Type objectType = TypeUtils.getVarExprType(node, table, method);

        //check if var is field
        for (Symbol field : table.getFields()) {
            if (field.getName().equals(node.get("name"))) {
                if(method.equals("main")){
                    addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                            "Field cannot be accessed in static main function"));
                }
            }
        }

        if (objectType.getName()==null){
            clearReports();
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                    "Wrong type declaration " + objectType.getName()));
        }
        return null;
    }

    private Void dealWithMemberAccess(JmmNode node, SymbolTable table) {
        String method = get_Caller_method(node);
        Type objectType = TypeUtils.getExprType(node.getJmmChild(0), table,method);

        if (objectType == null) {
            int line = NodeUtils.getLine(node);
            int column = NodeUtils.getColumn(node);
            String message = "objectType is null";
            addReport(Report.newError(Stage.SEMANTIC, line, column, message, null));
            return null;
        }

        String objectTypeName = objectType.getName();
        if (objectTypeName.equals("this") && table.getClassName().equals("main")) {
            int line = NodeUtils.getLine(node);
            int column = NodeUtils.getColumn(node);
            String message = "Found \"this\" in static main function";
            addReport(Report.newError(Stage.SEMANTIC, line, column, message, null));
            return null;
        }
        if (Arrays.asList("int", "boolean").contains(objectTypeName)) {
            int line = NodeUtils.getLine(node);
            int column = NodeUtils.getColumn(node);
            String message = "Object member access must be a reference but found " + objectTypeName;
            addReport(Report.newError(Stage.SEMANTIC, line, column, message, null));
            return null;
        }

        String methodName = node.get("id");
        Type accessedMethodType = table.getReturnType(methodName);

        if (accessedMethodType == null && table.getSuper() == null && !table.getImports().contains(objectTypeName)) {
            int line = NodeUtils.getLine(node);
            int column = NodeUtils.getColumn(node);
            String message = "Method accessed '" + methodName + "' not found";
            addReport(Report.newError(Stage.SEMANTIC, line, column, message, null));
            return null;
        }

        if (accessedMethodType == null) {
            int line = NodeUtils.getLine(node);
            int column = NodeUtils.getColumn(node);
            String message = "Method accessed '" + methodName + "' not found";
            addReport(Report.newError(Stage.SEMANTIC, line, column, message, null));
            return null;
        }

        List<Symbol> methodParams = table.getParameters(methodName);
        if (methodParams == null) {
            return null;
        }

        if ((node.getNumChildren() - 1) != methodParams.size()) {
            int line = NodeUtils.getLine(node);
            int column = NodeUtils.getColumn(node);
            String message = "Expected " + methodParams.size() + " parameters but received " + (node.getNumChildren() - 1) + " parameters";
            addReport(Report.newError(Stage.SEMANTIC, line, column, message, null));
            return null;
        }

        for (Symbol parameter : methodParams) {
            Type paramType = TypeUtils.getExprType(node.getJmmChild(1 + methodParams.indexOf(parameter)), table,null);
            Type expectedType = parameter.getType();

            if (!TypeUtils.areTypesAssignable(paramType, expectedType, table)) {
                int line = NodeUtils.getLine(node);
                int column = NodeUtils.getColumn(node);
                String message = "Expected parameter of type " + expectedType.getName() + " in method " + methodName + " but found " + paramType.getName();
                addReport(Report.newError(Stage.SEMANTIC, line, column, message, null));
            }
        }

        return null;
    }
    private Void Check_return(JmmNode node, SymbolTable table) {
        String method= get_Caller_method(node);
        Type objectType = TypeUtils.getExprType(node.getJmmChild(0), table, method);
        Type returntype=table.getReturnType(node.getParent().get("name"));
        if (objectType.getName()==null || returntype.getName()==null){
            clearReports();
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                    "Wrong Return type " + objectType.getName()));
            return null;
        }
        if (!objectType.getName().equalsIgnoreCase(returntype.getName())){
            clearReports();
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                    "Wrong Return type " + objectType.getName()));
        }
        return null;
    }
    private String get_Caller_method(JmmNode node){
        while (!(node.getKind()).equals("MethodDecl") && !(node.getKind()).equals("MainMethodDecl")){
            if(node.getParent()==null){
                return null;
            }
            node=node.getParent();
        }
        return node.get("name");
    }

    private Void dealWithArray(JmmNode node, SymbolTable table) {
        String method = get_Caller_method(node);
        Type arrayType = TypeUtils.getExprType(node, table,method);
        if (arrayType == null) {
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                    "Array is invalid"));
        }

        return null;
    }

    private Void dealWithCallExpr(JmmNode node, SymbolTable table){
        String method_called = node.get("funcname");
        if (table.getMethods().contains(method_called)){
            List<Symbol> methodParams = table.getParameters(method_called);

            if (methodParams.size()!=node.getChildren().size()) {
                int line = NodeUtils.getLine(node);
                int column = NodeUtils.getColumn(node);
                String message = "Expected " + methodParams.size() + " parameters but received " + (node.getChildren().size()) + " parameters";
                addReport(Report.newError(Stage.SEMANTIC, line, column, message, null));
                return null;

            }

            if (methodParams.isEmpty() && !node.getChildren().isEmpty()) {
                int line = NodeUtils.getLine(node);
                int column = NodeUtils.getColumn(node);
                String message = "Method " + method_called + " expects no parameters but received " + node.getChildren().size() + " parameters";
                addReport(Report.newError(Stage.SEMANTIC, line, column, message, null));
                return null;
            }

            Set<String> paramNames = new HashSet<>();

            for (int i = 0; i < methodParams.size(); i++) {
                Type paramType = TypeUtils.getExprType(node.getJmmChild(i), table,null);
                Type expectedType = methodParams.get(i).getType();

                if (!paramType.equals(expectedType)) {
                    int line = NodeUtils.getLine(node);
                    int column = NodeUtils.getColumn(node);
                    String message = "Expected parameter of type " + expectedType.getName() + " in method " + method_called + " but found " + paramType.getName();
                    addReport(Report.newError(Stage.SEMANTIC, line, column, message, null));
                }

                JmmNode paramNode = node.getJmmChild(i);
                String paramName = paramNode.get("paramname");

                if (!paramNames.add(paramName)) {
                    int line = NodeUtils.getLine(paramNode);
                    int column = NodeUtils.getColumn(paramNode);
                    String message = "Duplicate parameter name '" + paramName + "' in method call";
                    addReport(Report.newError(Stage.SEMANTIC, line, column, message, null));
                }
            }

        }else{
            if (table.getSuper() == null) {
                addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                        "Method not declared " + method_called));
            }
        }

        return null;
    }
    private Void dealWithMemberCallExpr(JmmNode node, SymbolTable table){
        String method = get_Caller_method(node);
        Type objectType = TypeUtils.getExprType(node.getJmmChild(0), table, method);
        if(TypeUtils.check_for_imports_type(objectType,table)){
            return null;
        }
        if (objectType.getName() == null) {
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                    "Object is invalid"));
        }
        String method_called = node.get("name");
        if (table.getMethods().contains(method_called)){
            List<Symbol> methodParams = table.getParameters(method_called);

            if (methodParams.size()!=(node.getChildren().size()-1) && !Objects.equals(methodParams.get(methodParams.size() - 1).getType().getName(), "_varargs")) {
                int line = NodeUtils.getLine(node);
                int column = NodeUtils.getColumn(node);
                String message = "Expected " + methodParams.size() + " parameters but received " + (node.getChildren().size() - 1) + " parameters";
                addReport(Report.newError(Stage.SEMANTIC, line, column, message, null));
                return null;
            }
            int j = 0;
            for (int i = 0; i < (node.getChildren().size()-1); i++) {
                Type paramType = TypeUtils.getExprType(node.getJmmChild(i+1), table,null);
                Type expectedType = methodParams.get(j).getType();
                if (Objects.equals(expectedType.getName(), "_varargs")) {
                    expectedType = new Type("int", false);
                    j--;
                }
                j++;

                if (!paramType.equals(expectedType)) {
                    int line = NodeUtils.getLine(node);
                    int column = NodeUtils.getColumn(node);
                    String message = "Expected parameter of type " + expectedType.getName() + " in method " + method_called + " but found " + paramType.getName();
                    addReport(Report.newError(Stage.SEMANTIC, line, column, message, null));
                }
            }

        }else{
            String super_class = table.getSuper();
            if (super_class ==null) {

                addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                        "Method not declared " + method_called));
            }
        }

        if (node.get("name").equals("length")) {
            Type objectType2 = TypeUtils.getExprType(node.getJmmChild(0), table, null);
            if (!objectType2.isArray()) {
                addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                        "Method 'length' can only be used on arrays"));
            }
            return null;
        }


        return null;
    }

    private Void dealWithIf(JmmNode node, SymbolTable table){
        String method = get_Caller_method(node);
        Type objectType = TypeUtils.getExprType(node.getJmmChild(0), table, method);
        if (!objectType.getName().equals("boolean")) {
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                    "If condition must be a boolean expression"));
        }
        return null;
    }
    private Void dealWithWhile(JmmNode node, SymbolTable table){
        String method = get_Caller_method(node);
        Type objectType = TypeUtils.getExprType(node.getJmmChild(0), table, method);
        if (!objectType.getName().equals("boolean")) {
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                    "While condition must be a boolean expression"));
        }
        return null;
    }

    private Void dealWithLength(JmmNode node, SymbolTable table) {
        String method = get_Caller_method(node);
        Type exprType = TypeUtils.getExprType(node.getJmmChild(0), table, method);

        if (exprType == null || exprType.getName() == null) {
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                    "Invalid type for expression"));
            return null;
        }
        if (!exprType.isArray()) {
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                    "Length can only be used on arrays"));
        }

        return null;
    }











}
