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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Analyser extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(Kind.METHOD_DECL, this::visitMethod);
        addVisit(Kind.ASSIGN_STMT, this::visitWhileStmt);
        addVisit(Kind.PROGRAM, this::dealWithProgram);
        addVisit(Kind.TYPE, this::dealWithType);
        addVisit(Kind.VAR_REF_EXPR, this::Check_decl);
        addVisit(Kind.RETURN_STMT, this::Check_return);
    }

    private Void dealWithProgram(JmmNode node, SymbolTable table) {
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Import")) {
                continue;
            }
            visit(child, table);
        }
        return null;
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table) {
        String typeName = varDecl.get("name");

        if (typeName.equals(TypeUtils.getIntTypeName())) {
            String message = "Cannot assign an integer value to a boolean variable";
            int line = varDecl.get("lineStart") != null ? Integer.parseInt(varDecl.get("lineStart")) : -1;
            int column = varDecl.get("colStart") != null ? Integer.parseInt(varDecl.get("colStart")) : -1;
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

        Type leftType = TypeUtils.getExprType(leftOperand, table);
        Type rightType = TypeUtils.getExprType(rightOperand, table);

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
                Type argType = TypeUtils.getExprType(argNode, table);

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
        if (node.getKind().equals("VARARGS")) {
            varArgsNodes.add(node);
        }
        for (JmmNode child : node.getChildren()) {
            collectVarArgsNodes(child, varArgsNodes);
        }
    }

    private Void visitWhileStmt(JmmNode whileNode, SymbolTable table) {
        JmmNode expression = whileNode.getChildren().get(0);
        JmmNode statement = whileNode.getChildren().get(1);



        visit(statement, table);

        return null;
    }

    private void checkWhileStatement(JmmNode expression, SymbolTable table) {
        Type exprType = TypeUtils.getExprType(expression, table);
        if (exprType.isArray() || !exprType.getName().equals("boolean")) {
            int line = expression.get("lineStart") != null ? Integer.parseInt(expression.get("lineStart")) : -1;
            int column = expression.get("colStart") != null ? Integer.parseInt(expression.get("colStart")) : -1;
            String message = "Invalid while condition";
            addReport(new Report(ReportType.WARNING, Stage.SEMANTIC, line, column, message));
        }
    }

    private Void dealWithType(JmmNode node, SymbolTable table) {
        if (node.getChildren().size() == 0) {
            return null;
        }
        Type arrayType = TypeUtils.getExprType(node.getChildren().get(0), table);
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
        Type objectType = TypeUtils.getVarExprType(node, table);
        if (objectType.getName()==null){
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                    "Variable not declared " + objectType.getName()));
        }
        return null;
    }

    private Void dealWithMemberAccess(JmmNode node, SymbolTable table) {
        Type objectType = TypeUtils.getExprType(node.getJmmChild(0), table);

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
            // Tratar o caso em que os parâmetros do método são nulos
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
            Type paramType = TypeUtils.getExprType(node.getJmmChild(1 + methodParams.indexOf(parameter)), table);
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
        Type objectType = TypeUtils.getExprType(node.getJmmChild(0), table);
        Type returntype=table.getReturnType(node.getParent().get("name"));
        if (!objectType.equals(returntype)){
            addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, NodeUtils.getLine(node), NodeUtils.getColumn(node),
                    "Variable not declared " + objectType.getName()));
        }
        return null;
    }



}
