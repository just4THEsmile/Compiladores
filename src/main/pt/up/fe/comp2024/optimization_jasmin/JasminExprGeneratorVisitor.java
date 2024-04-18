package pt.up.fe.comp2024.optimization_jasmin;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.Map;
import java.util.Objects;

public class JasminExprGeneratorVisitor extends PostorderJmmVisitor<StringBuilder, Void> {

    private static final String NL = "\n";

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

    }

    private Void visitIntegerLiteral(JmmNode integerLiteral, StringBuilder code) {
        code.append("ldc " + integerLiteral.get("value") + NL);
        return null;
    }
    private Void visitLengthExpr(JmmNode lengthExpr, StringBuilder code) {
        this.visit(lengthExpr.getJmmChild(0), code);
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
        var name = varRefExpr.get("name");
        var fields = table.getFields();
        // get register
        var reg = currentRegisters.get(name);
        var imports = table.getImports();
        Type t = TypeUtils.getVarExprType(varRefExpr, table, CurrentMethod);
        if ((TypeUtils.check_for_imports_type(t, table))) {
            return null;
        }
        for (var field : fields) {
            if (field.getName().equals(name)) {
                code.append("aload 0" + NL);
                code.append("getfield " + table.getClassName() + "/" + name + " " + getTypeToStr(field.getType()) + NL);
                return null;
            }
        }


        SpecsCheck.checkNotNull(reg, () -> "No register mapped for variable '" + name + "'");

        switch (t.getName()) {
            case "int","boolean" :
                code.append("iload ");
                break;
            default :
                code.append("aload ");
                break;
        }


        code.append(reg + NL);

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
            default -> throw new NotImplementedException(binaryExpr.get("op"));
        };

        // apply operation
        code.append(op).append(NL);

        return null;
    }
    private Void visitNotExpr(JmmNode notExpr, StringBuilder code) {
        code.append("ldc 1" + NL);
        code.append("ixor" + NL);
        return null;
    }
    private Void visitNewObject(JmmNode newObject, StringBuilder code) {
        var className = newObject.get("classname");
        for (var child : newObject.getChildren()) {
            this.visit(child, code);
        }
        code.append("new " + className + NL);
        if (className.equals(table.getClassName())) {
            code.append("dup" + NL);
            code.append("invokespecial " + className + "/<init>()V" + NL);
        }
        //code.append("invokespecial " + className + "/<init>()V" + NL);
        return null;
    }
    private Void visitMemberCallExpr(JmmNode memberCallExpr, StringBuilder code) {
        var methodName = memberCallExpr.get("name");
        var className = TypeUtils.getExprType(memberCallExpr.getJmmChild(0), table, CurrentMethod).getName();
        Type t = TypeUtils.getExprType(memberCallExpr.getJmmChild(0), table, CurrentMethod);
        if ((TypeUtils.check_for_imports_type(t, table)) && memberCallExpr.getJmmChild(0).getChildren().isEmpty()) {
            if(memberCallExpr.getNumChildren() == 1) {
                code.append("invokestatic " + className + "/" + methodName + "()");
                if (className.equals(table.getClassName())) {
                    code.append(getTypeReturnToStr(table.getReturnType(methodName)));
                }else {
                    code.append("V");
                }
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
            }else {
                code.append("V");
            }
            code.append(NL);
            return null;
        }else{
            if(memberCallExpr.getNumChildren() == 1) {
                code.append("invokevirtual " + className + "/" + methodName + "()");
                if (className.equals(table.getClassName())) {
                    var test= table.getReturnType(methodName);
                    code.append(getTypeReturnToStr(table.getReturnType(methodName)));
                }else {
                    code.append("V");
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
                string.append("L").append(table.getClassName()).append(";");
                break;

            default:
                string.append("L").append(type.getName()).append(";");
                return null;
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



}
