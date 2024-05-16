package pt.up.fe.comp2024.optimization_jasmin;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmNode;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.Map;
import java.util.Objects;

public class JasminExprGeneratorVisitor extends PostorderJmmVisitor<StringBuilder, Void> {

    private static final String NL = "\n";
    private static int  label_number = 0;
    private final Map<String, Integer> currentRegisters;
    private final SymbolTable table;
    private final String CurrentMethod;

    public JasminExprGeneratorVisitor(Map<String, Integer> currentRegisters,SymbolTable table, String CurrentMethod) {
        this.currentRegisters = currentRegisters;
        this.table = table;
        this.CurrentMethod = CurrentMethod;
    }

    @Override
    protected void buildVisitor() {
        // Using strings to avoid compilation problems in projects that
        // might no longer have the equivalent enums in Kind class.
        addVisit("MemberCallExpr", this::visitMemberCallExpr);
        addVisit("MethodCallExpr", this::visitMethodCallExpr);
        addVisit("IntegerLiteral", this::visitIntegerLiteral);
        addVisit("VarRefExpr", this::visitVarRefExpr);
        addVisit("BinaryExpr", this::visitBinaryExpr);
        addVisit("BooleanLiteral", this::visitBooleanLiteral);
        addVisit("NewObject", this::visitNewObject);
        addVisit("ThisRefExpr", this::visitThisRefExpr);
        addVisit("NotExpr", this::visitNotExpr);
        addVisit("ParenExpr", this::visitParenExpr);
        addVisit("LengthExpr", this::visitLengthExpr);
        addVisit("NewIntArray", this::visitNewArray);
        addVisit("ArrayAccessExpr", this::visitArrayAccessExpr);

    }

    private Void visitArrayAccessExpr(JmmNode arrayAccessExpr, StringBuilder code) {

            code.append("iaload" + NL);
            return null;
    }

    private Void visitNewArray(JmmNode newArray, StringBuilder code){

        String type = newArray.get("name");
        code.append("newarray ").append(type).append(NL);
        return null;

    }

    private Void visitIntegerLiteral(JmmNode integerLiteral, StringBuilder code) {
        code.append("ldc " + integerLiteral.get("value") + NL);
        return null;
    }
    private Void visitLengthExpr(JmmNode lengthExpr, StringBuilder code) {
        code.append("arraylength" + NL);
        return null;
    }
    private Void visitParenExpr(JmmNode parenExpr, StringBuilder code) {
        return null;
    }
    private Void visitBooleanLiteral(JmmNode booleanLiteral, StringBuilder code) {
        if (booleanLiteral.get("value").equals("true"))
            code.append("ldc " + "1" + NL);
        else
            code.append("ldc " + "0" + NL);
        return null;
    }
    private Void visitThisRefExpr(JmmNode thisRefExpr, StringBuilder code) {
        code.append("aload 0" + NL);
        return null;
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, StringBuilder code) {
        String name = varRefExpr.get("name");
        var fields = table.getFields();
        // get register
        var reg = currentRegisters.get(name);
        var imports = table.getImports();
        Type t = TypeUtils.getVarExprType(varRefExpr, table, CurrentMethod);

        if ((TypeUtils.check_for_imports_type(new Type(name,false), table))) {
            return null;
        }
        for (var field : fields) {
            if (field.getName().equals(name)) {
                code.append("aload 0" + NL);
                code.append("getfield " + table.getClassName() + "/" + name+" " + getTypeToStr(field.getType()) + NL);
                return null;
            }
        }


        SpecsCheck.checkNotNull(reg, () -> "No register mapped for variable '" + name + "'");

        if (t.isArray()){
            code.append("aload ");
            code.append(currentRegisters.get(name) + NL);
            return null;
        }
        switch (t.getName()) {
            case "int","boolean" :
                code.append("iload ");
                code.append(currentRegisters.get(name) + NL);
                break;
            default :
                code.append("aload ");
                code.append(currentRegisters.get(name) + NL);
                break;
        }


        return null;
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, StringBuilder code) {

        // since this is a post-order visitor that automatically visits the children
        // we can assume the value for the operation are already loaded in the stack

        // get the operation
        var op = switch (binaryExpr.get("op")) {
            case "+" -> "iadd";
            case "*" -> "imul";
            case "-" -> "isub";
            case "/" -> "idiv";
            case "&&"-> "iand";
            case "<" -> {
                StringBuilder string=new StringBuilder();
                int local_label=label_number;
                label_number++;
                string.append("isub" + NL);
                string.append("iflt " + "cmp_label_true_" + local_label + NL);
                string.append("iconst_0" + NL);
                string.append("goto " + "cmp_label_end_" + local_label + NL);
                string.append("cmp_label_true_" + local_label + ":" + NL);
                string.append("iconst_1" + NL);
                string.append("cmp_label_end_" + local_label + ":" + NL);


                yield  string;
            }
            default -> throw new NotImplementedException(binaryExpr.get("op"));
        };

        // apply operation
        code.append(op).append(NL);

        return null;
    }
    private Void visitNotExpr(JmmNode notExpr, StringBuilder code) {
        code.append("iconst_1" + NL);
        code.append("ixor" + NL);
        return null;
    }
    private Void visitNewObject(JmmNode newObject, StringBuilder code) {
        var className = get_parsed_class(newObject.get("classname"));


        code.append("new " + className + NL);
        code.append("dup" + NL);
        code.append("invokespecial " + className + "/<init>()V" + NL);

        // TODO : WHATCHOUT FOR POP idfk
        if(has_parent_stmt_pop_check(newObject)){
            code.append("pop" + NL);
        }

        //code.append("invokespecial " + className + "/<init>()V" + NL);
        return null;
    }
    private Void visitMemberCallExpr(JmmNode memberCallExpr, StringBuilder code) {
        var methodName = memberCallExpr.get("name");
        var className = get_parsed_class(TypeUtils.getExprType(memberCallExpr.getJmmChild(0), table, CurrentMethod).getName());
        Type t = TypeUtils.getExprType(memberCallExpr.getJmmChild(0), table, CurrentMethod);
        if ((TypeUtils.check_for_imports_type(t, table)) && memberCallExpr.getJmmChild(0).getChildren().isEmpty()) {
            if(memberCallExpr.getNumChildren() == 1) {
                code.append("invokestatic " + className + "/" + methodName + "()");
                if (className.equals(table.getClassName())) {
                    code.append(getTypeReturnToStr(table.getReturnType(methodName)));
                }else if(has_ancertor_assign(memberCallExpr)!=null){
                    code.append(getTypeToStr(has_ancertor_assign(memberCallExpr)));
                }else {
                    code.append("V");
                }
                code.append(NL);


                return null;
            }
            var children = memberCallExpr.getChildren();
            code.append("invokestatic " + className + "/" + methodName + "(");
            for (int i = 1; i<children.size(); i++) {
                t = TypeUtils.getExprType(children.get(i), table, CurrentMethod);
                code.append(getTypeToStr(t));
            }
            code.append(")");
            if (className.equals(table.getClassName())) {
                code.append(getTypeReturnToStr(table.getReturnType(methodName)));
            }else if(has_ancertor_assign(memberCallExpr)!=null){
                code.append(getTypeToStr(has_ancertor_assign(memberCallExpr)));
            } else {
                code.append("V").append(NL);
            }
            code.append(NL);
            return null;
        }else{
            if(memberCallExpr.getNumChildren() == 1) {
                code.append("invokevirtual " + className + "/" + methodName + "()");
                if (className.equals(table.getClassName())) {
                    var test= table.getReturnType(methodName);
                    code.append(getTypeReturnToStr(table.getReturnType(methodName)));
                }else if(has_ancertor_assign(memberCallExpr)!=null){
                    code.append(getTypeToStr(has_ancertor_assign(memberCallExpr)));
                }else {
                    code.append("V").append(NL);
                }
                code.append(NL);

                return null;
            }
            var children = memberCallExpr.getChildren();
            code.append("invokevirtual " + className + "/" + methodName + "(");
            for (int i = 1; i<children.size(); i++) {
                t = TypeUtils.getExprType(children.get(i), table, CurrentMethod);
                code.append(getTypeToStr(t));
            }
            code.append(")");
            if (className.equals(table.getClassName())) {
                code.append(getTypeReturnToStr(table.getReturnType(methodName)));
            }else if(has_ancertor_assign(memberCallExpr)!=null){
                code.append(getTypeToStr(has_ancertor_assign(memberCallExpr)));
            }else {
                code.append("V");
            }
            code.append(NL);



            return null;
        }

    }


    private Void visitMethodCallExpr(JmmNode methodCallExpr, StringBuilder code) {
        var methodName = methodCallExpr.get("funcname");
        var className = table.getClassName();
        var children = methodCallExpr.getChildren();
        code.append("aload 0" + NL);
        if (children.isEmpty()) {
            code.append("invokevirtual " + className + "/" + methodName + "()");
            code.append(getTypeReturnToStr(table.getReturnType(methodName)));
            code.append(NL);
            return null;
        }
        code.append("invokevirtual " + className + "/" + methodName + "(");
        for (int i = 0; i<children.size(); i++) {
            var t = TypeUtils.getExprType(children.get(i), table, CurrentMethod);
            code.append(getTypeToStr(t));
        }
        code.append(")");
        code.append(getTypeReturnToStr(table.getReturnType(methodName)));
        code.append(NL);

        return null;
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
                string.append("L").append(get_parsed_class(table.getClassName())).append(";");
                break;

            default:
                string.append("L").append(get_parsed_class(type.getName())).append(";");
        }
        return string.toString();
    }
    private String getTypeReturnToStr(pt.up.fe.comp.jmm.analysis.table.Type type){
        StringBuilder string=new StringBuilder();
        if (type.getName()==null){
            string.append("V");
            return string.toString();
        }
        if(Objects.equals(type.getName(), "void")){
            string.append("V");
            return string.toString();
        }
        return getTypeToStr(type);
    }

    private String get_parsed_class(String class_name){
        for(String import_class : table.getImports()){
            if(import_class.contains(class_name)){
                String s=import_class;
                // remove , from import
                s = s.replace(", ", "/").replace("[", "").replace("]", "");
                return s;
            }
        }
        return class_name;
    }


    private Type has_ancertor_assign(JmmNode node){
        var parent = node.getParent();
        while (parent!=null){
            if (parent.getKind().equals("AssignStmt")){
                return TypeUtils.getVarExprType(parent.getJmmChild(0), table, CurrentMethod);
            }
            parent = parent.getParent();
        }
        return null;

    }

    private Boolean has_parent_stmt_pop_check(JmmNode node){
        var parent = node.getParent();
            if (parent.getKind().equals("IfStmt") || parent.getKind().equals("WhileStmt") || parent.getKind().equals("BlockStm") || parent.getKind().equals("ExprStmt") ){
                return true;
            }else{
                return false;
            }
    }


}
