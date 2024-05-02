package pt.up.fe.comp2024.optimization_jasmin;

import org.specs.comp.ollir.ClassType;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.utilities.StringLines;
import pt.up.fe.comp2024.symboltable.JmmSymbolTableBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class JasminGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final SymbolTable table;

    private JasminExprGeneratorVisitor exprGenerator;

    private String currentMethod;
    private int nextRegister;

    private Map<String, Integer> currentRegisters;

    public JasminGeneratorVisitor(SymbolTable table) {
        this.table = table;
        this.exprGenerator = null;
        currentMethod = null;
        nextRegister = -1;
        currentRegisters = null;
    }


    @Override
    protected void buildVisitor() {
        // Using strings to avoid compilation problems in projects that
        // might no longer have the equivalent enums in Kind class.

        addVisit("Program", this::visitProgram);
        addVisit("ClassDecl", this::visitClassDecl);
        addVisit("MethodDecl", this::visitMethodDecl);
        addVisit("MainMethodDecl", this::visitMainMethodDecl);
        addVisit("AssignStmt", this::visitAssignStmt);
        addVisit("ReturnStmt", this::visitReturnStmt);
        addVisit("BlockStmt", this::visitBlockStmt);
        addVisit("ExprStmt", this::visitExprStmt);

    }


    private String visitProgram(JmmNode program, Void unused) {
        var imports = table.getImports();
        // TODO : IMPLEMENT IMPORTS
        // Get class decl node
        var classDecl = program.getChildren("ClassDecl").get(0);

        return visit(classDecl);
    }

    private String visitClassDecl(JmmNode classDecl, Void unused) {
        var code = new StringBuilder();

        // generate class name
        var className = table.getClassName();
        code.append(".class public ").append(className).append(NL).append(NL);

        if(table.getSuper() != null) {
            code.append(".super ").append(get_parsed_class(table.getSuper())).append(NL).append(NL);
        }else{
            code.append(".super java/lang/Object").append(NL);
        }
        for(Symbol field : table.getFields()){
            code.append(".field ").append("public ").append(field.getName()).append(" ").append(this.getTypeToStr(field.getType())).append(NL);
        }
        // to find constructor
        boolean found=false;
        for (var method : classDecl.getChildren()) {
            if(method.getKind().equals("VarDecl")){
                continue;
            }else{
                if(method.get("name").equals(table.getClassName())){
                    code.append(".method public <init>()V").append(NL);
                    code.append(TAB).append("aload_0").append(NL);
                    code.append(TAB).append("invokespecial ").append(get_parsed_class(table.getSuper())).append("/<init>(");
                    for(Symbol param: table.getParameters(method.get("name"))){
                        code.append(getTypeToStr(param.getType()));
                    }
                    code.append(")V").append(NL);
                    code.append(TAB).append("return").append(NL);
                    code.append(".end method").append(NL).append(NL);
                    found=true;
                    continue;
                }
            }
            code.append(visit(method));
        }
        if (!found){
            // generate a single constructor method
            code.append( """
                ;default constructor
                .method public <init>()V
                    aload_0
                    invokespecial""");
            if(table.getSuper() != null) {
                code.append(" ").append(get_parsed_class(table.getSuper())).append("""
            /<init>()V
            return
                .end method
            """);
            }else{
                code.append( """
                 java/lang/Object/<init>()V
                    return
                .end method
                """);
            }
        }



        return code.toString();
    }

    private String visitMainMethodDecl(JmmNode mainMethodDecl, Void unused) {
    var methodName = "main";

    // set method
    currentMethod = methodName;

    // set next register that can be used
    // if method is static, then can start at 0
    // if method is not static, 0 contains 'this', and must start at 1
    // for the initial language, there are no static methods
    nextRegister = 0; //main is always static

    // initialize register map and set parameters
    currentRegisters = new HashMap<>();
    if (mainMethodDecl.hasAttribute("arg")){
        currentRegisters.put(mainMethodDecl.get("arg"), nextRegister);
        nextRegister++;
    }
    for (var local : table.getLocalVariables(currentMethod)){
        currentRegisters.put(local.getName(), nextRegister);
        nextRegister++;
    }

    exprGenerator = new JasminExprGeneratorVisitor(currentRegisters,table,currentMethod);

    var code = new StringBuilder();

    // calculate modifier
    var modifier = mainMethodDecl.getObject("isPublic", Boolean.class) ? "public " : "";
    var modifierStatic = mainMethodDecl.getObject("isStatic", Boolean.class) ? "static " : "";


    // TODO: Hardcoded param types and return type, needs to be expanded
    code.append("\n.method ").append(modifier).append(modifierStatic).append(methodName).append("(");

    code.append("[Ljava/lang/String;");

    code.append(")V").append(NL);
    // Add limits
    code.append(TAB).append(".limit stack 99").append(NL);
    code.append(TAB).append(".limit locals 99").append(NL);


    for (var stmt : mainMethodDecl.getChildren("Stmt")) {
        // Get code for statement, split into lines and insert the necessary indentation
        var instCode = StringLines.getLines(visit(stmt)).stream()
                .collect(Collectors.joining(NL + TAB, TAB, NL));

        code.append(instCode);
    }
    code.append(TAB).append("return").append(NL);

    code.append(".end method\n");

    // reset information
    exprGenerator = null;
    nextRegister = -1;
    currentRegisters = null;
    currentMethod = null;

    return code.toString();
}


    private String visitMethodDecl(JmmNode methodDecl, Void unused) {
        var methodName = methodDecl.get("name");


        // set method
        currentMethod = methodName;

        // set next register that can be used
        // if method is static, then can start at 0
        // if method is not static, 0 contains 'this', and must start at 1
        // for the initial language, there are no static methods

        if(Objects.equals(methodDecl.get("isStatic"), "true")){
            nextRegister = 0;
        }else{
            nextRegister = 1;
        }

        // initialize register map and set parameters
        currentRegisters = new HashMap<>();
        var params = methodDecl.getChildren("Params");
        var varArgs = methodDecl.getChildren("VarArgs");
        if(!params.isEmpty()) {
            var paramList = params.get(0);
            for (var param : paramList.getChildren("Param")) {
                currentRegisters.put(param.get("name"), nextRegister);
                nextRegister++;
            }
            // varargs
            if(paramList.hasAttribute("val")){
                currentRegisters.put(paramList.get("val"), nextRegister);
                nextRegister++;
            }
        } else if (!varArgs.isEmpty()) {
            currentRegisters.put(varArgs.get(0).get("val"), nextRegister);
            nextRegister++;
        }
        for (var local : table.getLocalVariables(currentMethod)){
            currentRegisters.put(local.getName(), nextRegister);
            nextRegister++;
        }


        exprGenerator = new JasminExprGeneratorVisitor(currentRegisters,table,currentMethod);

        var code = new StringBuilder();

        // calculate modifier
        var modifier = methodDecl.getObject("isPublic", Boolean.class) ? "public " : "";
        var modifierStatic = methodDecl.getObject("isStatic", Boolean.class) ? "static " : "";


        // TODO: Hardcoded param types and return type, needs to be expanded
        code.append("\n.method ").append(modifier).append(modifierStatic).append(methodName).append("(");

        if(!params.isEmpty()) {
            var paramList = params.get(0);
            for (var param : paramList.getChildren("Param")) {
                 Type t= TypeUtils.getVarExprType(param,table,currentMethod);
                code.append(getTypeToStr(t));
            }
            // varargs
            if(paramList.hasAttribute("val")){
                code.append("[I");
            }
        }else if (!varArgs.isEmpty()) {
            code.append("[I");
        }
        code.append(")").append(get_parsed_class(getTypeReturnToStr(table.getReturnType(currentMethod)))).append(NL);

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var stmt : methodDecl.getChildren("Stmt")) {
            // Get code for statement, split into lines and insert the necessary indentation
            var instCode = StringLines.getLines(visit(stmt)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }
        if (table.getReturnType(currentMethod).getName().equals("void")){
            code.append(TAB).append("return").append(NL);
        }

        code.append(".end method\n");

        // reset information
        exprGenerator = null;
        nextRegister = -1;
        currentRegisters = null;
        currentMethod = null;

        return code.toString();
    }

    private String visitAssignStmt(JmmNode assignStmt, Void unused) {
        var code = new StringBuilder();

        // generate code that will put the value on the right on top of the stack


        // store value in top of the stack in destination
        var lhs = assignStmt.getChild(0);
        SpecsCheck.checkArgument(lhs.isInstance("VarRefExpr"), () -> "Expected a node of type 'VarRefExpr', but instead got '" + lhs.getKind() + "'");

        var destName = lhs.get("name");

        // get register
        var reg = currentRegisters.get(destName);

        // If no mapping, variable has not been assigned yet, create mapping
        if (reg == null) {
            reg = nextRegister;
            currentRegisters.put(destName, reg);
            nextRegister++;
        }
        var fieldType = table.getFields();
        for (Symbol field : fieldType){
            if(field.getName().equals(destName)){
                code.append("aload 0").append(NL);
                exprGenerator.visit(assignStmt.getChild(1), code);
                code.append("putfield ").append(table.getClassName()).append("/").append(destName).append(" ").append(getTypeToStr(field.getType())).append(NL);
                return code.toString();
            }
        }

        exprGenerator.visit(assignStmt.getChild(1), code);
        Type t = TypeUtils.getVarExprType(assignStmt.getJmmChild(0),table,currentMethod);
        if (t.getName().equals("int") || t.getName().equals("boolean")) {
            code.append("istore ").append(reg).append(NL);
        }else{
            code.append("astore ").append(reg).append(NL);
        }

        return code.toString();
    }

    private String visitReturnStmt(JmmNode returnStmt, Void unused) {

        var code = new StringBuilder();
        Type returnType = table.getReturnType(currentMethod);

        switch (returnType.getName()){
            case "void":
                exprGenerator.visit(returnStmt.getChild(0), code);
                code.append("return").append(NL);
                break;
            case "int", "boolean":
                exprGenerator.visit(returnStmt.getChild(0), code);
                code.append("ireturn").append(NL);
                break;
            default:
                exprGenerator.visit(returnStmt.getChild(0), code);
                code.append("areturn").append(NL);
                break;
        }


        return code.toString();
    }

    private String visitExprStmt(JmmNode exprStmt, Void unused) {
        var code = new StringBuilder();

        // generate code for expression
        exprGenerator.visit(exprStmt.getChild(0), code);

        return code.toString();
    }

    private String visitBlockStmt(JmmNode blockStmt, Void unused) {
        var code = new StringBuilder();

        for (var stmt : blockStmt.getChildren("Stmt")) {
            // Get code for statement, split into lines and insert the necessary indentation
            var instCode = StringLines.getLines(visit(stmt)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }

        return code.toString();
    }

    private String getTypeToStr(pt.up.fe.comp.jmm.analysis.table.Type type){
        StringBuilder string=new StringBuilder();
        if(type.isArray()){
            string.append("[");
            }
        switch (type.getName()){
            case "int":
                string.append("I");
                break;
            case "_varargs":
                string.append("I");
                break;
            case "boolean":
                string.append("Z");
                break;

            case "string":
                string.append("Ljava/lang/String;");
                break;
            case "this":
                string.append("L").append(table.getClassName()).append(";");
                break;

            default:
                string.append("L").append(get_parsed_class(type.getName())).append(";");
        }
        return string.toString();
    }
    private String getTypeReturnToStr(pt.up.fe.comp.jmm.analysis.table.Type type){
        StringBuilder string=new StringBuilder();
        if(Objects.equals(type.getName(), "void")){
            string.append("V");
            return string.toString();
        }
        return getTypeToStr(type);
    }

    private String get_parsed_class(String class_name){
        if (class_name==null){
            return "java/lang/Object";
        }
        for(String import_class : table.getImports()){
            if(import_class.endsWith(class_name)){
                String s=import_class;
                // remove , from import
                s = s.replace(", ", "/").replace("[", "").replace("]", "");
                return s;
            }
        }
        return class_name;
    }


}