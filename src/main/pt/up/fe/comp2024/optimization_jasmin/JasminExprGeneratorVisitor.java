package pt.up.fe.comp2024.optimization_jasmin;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmNode;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JasminExprGeneratorVisitor extends PostorderJmmVisitor<StringBuilder, Void> {

    private static final String NL = "\n";
    private static int  label_number = 0;
    private static int max_reg_val;
    private static Map<String, Integer> currentRegisters;
    private final SymbolTable table;
    private final String CurrentMethod;
    private int max_stack_num;
    private int stack_size;


    public JasminExprGeneratorVisitor(Map<String, Integer> currentRegisters,SymbolTable table, String CurrentMethod) {
        JasminExprGeneratorVisitor.currentRegisters = currentRegisters;
        this.table = table;
        this.CurrentMethod = CurrentMethod;
        max_reg_val = currentRegisters.size();
        this.max_stack_num = 0;
        this.stack_size = 0;
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
        addVisit("Array", this::visitArray);

    }
    private Void visitArray(JmmNode array, StringBuilder code) {
        int num_of_reg=max_reg_val;
        max_reg_val++;
        max_reg_val++;
        add_stack_size(1);
        code.append(optimized_literal(array.getChildren().size()) + NL); // size of array
        code.append("newarray int" + NL);
        code.append("astore" + parse_with_under(num_of_reg) + NL);

        for (int i = array.getChildren().size()-1; i>=0; i--) {
            add_stack_size(1); // TODO : NOT sure
            // remove the top value to put the index then add it again
            code.append("istore"+parse_with_under(num_of_reg+1)).append(NL);
            code.append("aload"+parse_with_under(num_of_reg)).append(NL);
            code.append(optimized_literal(i)).append(NL);
            code.append("iload"+parse_with_under(num_of_reg+1)).append(NL);
            code.append("iastore" + NL).append(NL);
            sub_stack_size(1);
        }
        sub_stack_size(array.getChildren().size());
        code.append("aload"+parse_with_under(num_of_reg)).append(NL);

        if(has_parent_stmt_pop_check(array)){
            sub_stack_size(1);
            code.append("pop" + NL);
        }
        return null;
    }

    private Void visitArrayAccessExpr(JmmNode arrayAccessExpr, StringBuilder code) {
            sub_stack_size(1);
            code.append("iaload" + NL);
            if(has_parent_stmt_pop_check(arrayAccessExpr)){
                sub_stack_size(1);
                code.append("pop" + NL);
            }
            return null;
    }

    private Void visitNewArray(JmmNode newArray, StringBuilder code){

        String type = newArray.get("name");
        code.append("newarray ").append(type).append(NL);
        if(has_parent_stmt_pop_check(newArray)){
            sub_stack_size(1);
            code.append("pop" + NL);
        }
        return null;

    }

    private Void visitIntegerLiteral(JmmNode integerLiteral, StringBuilder code) {
        if(has_add_parent(integerLiteral)){
            return null;
        }
        int value = Integer.valueOf(integerLiteral.get("value"));
        add_stack_size(1);
        if(value>=0 && value<=5){
            code.append("iconst_" + value + NL);
        } else if (value >= -128 && value <= 127) {
            code.append("bipush " + value + NL);
        } else if (value >= -32768 && value <= 32767) {
            code.append("sipush " + value + NL);
        } else {
            code.append("ldc " + value + NL);

        }

        if(has_parent_stmt_pop_check(integerLiteral)){
            sub_stack_size(1);
            code.append("pop" + NL);
        }
        return null;
    }
    private Void visitLengthExpr(JmmNode lengthExpr, StringBuilder code) {
        code.append("arraylength" + NL);
        if(has_parent_stmt_pop_check(lengthExpr)){
            sub_stack_size(1);
            code.append("pop" + NL);
        }
        return null;
    }
    private Void visitParenExpr(JmmNode parenExpr, StringBuilder code) {
        return null;
    }
    private Void visitBooleanLiteral(JmmNode booleanLiteral, StringBuilder code) {
        add_stack_size(1);
        if (booleanLiteral.get("value").equals("true"))
            code.append("iconst_1" + NL);
        else
            code.append("iconst_0" + NL);
        if(has_parent_stmt_pop_check(booleanLiteral)){
            sub_stack_size(1);
            code.append("pop" + NL);
        }
        return null;
    }
    private Void visitThisRefExpr(JmmNode thisRefExpr, StringBuilder code) {
        add_stack_size(1);
        code.append("aload_0" + NL);
        if(has_parent_stmt_pop_check(thisRefExpr)){
            sub_stack_size(1);
            code.append("pop" + NL);
        }
        return null;
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, StringBuilder code) {
        if(has_add_parent(varRefExpr)){
            return null;
        }
        add_stack_size(1);
        String name = varRefExpr.get("name");
        var fields = table.getFields();
        // get register
        var reg = currentRegisters.get(name);
        var imports = table.getImports();
        Type t = TypeUtils.getVarExprType(varRefExpr, table, CurrentMethod);

        if ((TypeUtils.check_for_imports_type(new Type(name,false), table))) {
            return null;
        }
        if(name.equals(table.getClassName())){/*
            code.append("aload 0" + NL);

            if(has_parent_stmt_pop_check(varRefExpr)){
                sub_stack_size(1);
                code.append("pop" + NL);
            }*/
            return null;
        }
        if(reg==null){
            for (var field : fields) {
                if (field.getName().equals(name)) {
                    code.append("aload_0" + NL);
                    code.append("getfield " + table.getClassName() + "/" + name+" " + getTypeToStr(field.getType()) + NL);

                    if(has_parent_stmt_pop_check(varRefExpr)){
                        sub_stack_size(1);
                        code.append("pop" + NL);
                    }
                    return null;
                }
            }
        }


        SpecsCheck.checkNotNull(reg, () -> "No register mapped for variable '" + name + "'");

        if (t.isArray()){
            code.append("aload");
            code.append(parse_with_under(currentRegisters.get(name)) + NL);
            if(has_parent_stmt_pop_check(varRefExpr)){
                sub_stack_size(1);
                code.append("pop" + NL);
            }

            return null;
        }
        switch (t.getName()) {
            case "int","boolean" :
                code.append("iload");
                code.append(parse_with_under(currentRegisters.get(name)) + NL);
                if(has_parent_stmt_pop_check(varRefExpr)){
                    sub_stack_size(1);
                    code.append("pop" + NL);
                }
                break;
            default :
                code.append("aload");
                code.append(parse_with_under(currentRegisters.get(name)) + NL);
                if(has_parent_stmt_pop_check(varRefExpr)){
                    sub_stack_size(1);
                    code.append("pop" + NL);
                }

                break;
        }


        return null;
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, StringBuilder code) {

        // since this is a post-order visitor that automatically visits the children
        // we can assume the value for the operation are already loaded in the stack

        // get the operation
        if(!binaryExpr.get("op").equals("+") ){
            sub_stack_size(1);
        }
        var op = switch (binaryExpr.get("op")) {
            case "+" -> add_optimization(binaryExpr);
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
        if(has_parent_stmt_pop_check(binaryExpr)){
            sub_stack_size(1);
            code.append("pop" + NL);
        }

        return null;
    }
    private Void visitNotExpr(JmmNode notExpr, StringBuilder code) {
        add_stack_size(1);
        sub_stack_size(1);
        code.append("iconst_1" + NL);
        code.append("ixor" + NL);
        if(has_parent_stmt_pop_check(notExpr)){
            sub_stack_size(1);
            code.append("pop" + NL);
        }
        return null;
    }
    private Void visitNewObject(JmmNode newObject, StringBuilder code) {
        var className = get_parsed_class(newObject.get("classname"));

        add_stack_size(2);
        code.append("new " + className + NL);
        code.append("dup" + NL);
        code.append("invokespecial " + className + "/<init>()V" + NL);
        sub_stack_size(1);

        // TODO : WHATCHOUT FOR POP
        if(has_parent_stmt_pop_check(newObject)){
            sub_stack_size(1);
            code.append("pop" + NL);
        }

        //code.append("invokespecial " + className + "/<init>()V" + NL);
        return null;
    }
    private Void visitMemberCallExpr(JmmNode memberCallExpr, StringBuilder code) {
        var methodName = memberCallExpr.get("name");
        var className = get_parsed_class(TypeUtils.getExprType(memberCallExpr.getJmmChild(0), table, CurrentMethod).getName());
        Type t = TypeUtils.getExprType_Ollir(memberCallExpr.getJmmChild(0), table, CurrentMethod);
        if (t.getName().charAt(t.getName().length()-1)=='_' ) {
            String static_class= t.getName().substring(0, t.getName().length()-1);
            if( static_class==table.getClassName()){
                List< Symbol> params =table.getParameters(methodName);
                if(params==null){
                    //Do nothing
                }else if(params.isEmpty()){
                    //Do nothing
                }else {


                    for (Symbol s : params) {
                        if (s.getType().getName().equals("_varargs")) {
                            int num_of_reg = max_reg_val;
                            max_reg_val++;
                            max_reg_val++;
                            int non_var_args = params.size() - 1;
                            int varg_args_num = memberCallExpr.getNumChildren() - 1 - non_var_args;
                            add_stack_size(1);
                            code.append(optimized_literal(varg_args_num) + NL);
                            code.append("newarray int" + NL);
                            code.append("astore" + parse_with_under(num_of_reg) + NL);
                            for (int i = varg_args_num - 1; i >= 0; i--) {
                                add_stack_size(1);
                                // remove the top value to put the index then add it again
                                code.append("istore" + parse_with_under(num_of_reg + 1)).append(NL);
                                code.append("aload" + parse_with_under(num_of_reg)).append(NL);
                                code.append(optimized_literal(i)).append(NL);
                                code.append("iload" + parse_with_under(num_of_reg + 1)).append(NL);
                                code.append("iastore" + NL).append(NL);
                                sub_stack_size(1);
                            }
                            sub_stack_size(varg_args_num);
                            code.append("aload" + parse_with_under(num_of_reg)).append(NL);
                        }
                    }
                }
            }
            if(memberCallExpr.getNumChildren() == 1) {
                sub_stack_size(1);
                code.append("invokestatic " + className + "/" + methodName + "()");
                if (className.equals(table.getClassName())) {
                    code.append(getTypeReturnToStr(table.getReturnType(methodName))).append(NL);
                }else if(has_ancertor_assign(memberCallExpr)!=null){
                    code.append(getTypeToStr(has_ancertor_assign(memberCallExpr)));
                    add_stack_size(1);
                }else {
                    code.append("V").append(NL);
                }
                code.append(NL);




                return null;
            }
            var children = memberCallExpr.getChildren();
            sub_stack_size(1);
            code.append("invokestatic " + className + "/" + methodName + "(");
            for (int i = 1; i<children.size(); i++) {
                t = TypeUtils.getExprType(children.get(i), table, CurrentMethod);
                code.append(getTypeToStr(t));
            }
            sub_stack_size(children.size()-1);
            code.append(")");
            if (className.equals(table.getClassName())) {
                code.append(getTypeReturnToStr(table.getReturnType(methodName))).append(NL);
            }else if(has_ancertor_assign(memberCallExpr)!=null){
                code.append(getTypeToStr(has_ancertor_assign(memberCallExpr)));
                add_stack_size(1);
            } else {
                code.append("V").append(NL);
            }
            code.append(NL);
            return null;
        }else{
            if(memberCallExpr.getNumChildren() == 1) {
                sub_stack_size(1);
                code.append("invokevirtual " + className + "/" + methodName + "()");
                if (className.equals(table.getClassName())) {
                    var test= table.getReturnType(methodName);
                    code.append(getTypeReturnToStr(table.getReturnType(methodName))).append(NL);
                    add_stack_size(1);
                    if(has_parent_stmt_pop_check( memberCallExpr) && !getTypeReturnToStr(table.getReturnType(methodName)).equals("V")){
                        code.append("pop" + NL);
                        sub_stack_size(1);
                    }
                }else if(has_ancertor_assign(memberCallExpr)!=null){
                    code.append(getTypeToStr(has_ancertor_assign(memberCallExpr)));
                    add_stack_size(1);
                }else {
                    code.append("V").append(NL);
                }
                code.append(NL);

                return null;
            }
            Type t1 = TypeUtils.getExprType(memberCallExpr.getJmmChild(0), table, CurrentMethod);
            if (memberCallExpr.getJmmChild(0).getKind().equals("ThisRefExpr") || t1.getName().equals(table.getClassName())) {
                List< Symbol> params =table.getParameters(methodName);
                for(Symbol s : params){
                    if (s.getType().getName().equals("_varargs")){
                        int num_of_reg = max_reg_val;
                        max_reg_val++;
                        max_reg_val++;
                        int non_var_args = params.size()-1;
                        int varg_args_num = memberCallExpr.getNumChildren()-1-non_var_args;
                        add_stack_size(1);
                        code.append(optimized_literal(varg_args_num) + NL);
                        code.append("newarray int" + NL);
                        code.append("astore" + parse_with_under(num_of_reg) + NL);
                        for (int i=varg_args_num-1;i>=0;i--){
                            add_stack_size(1);
                            // remove the top value to put the index then add it again
                            code.append("istore"+parse_with_under(num_of_reg+1)).append(NL);
                            code.append("aload"+parse_with_under(num_of_reg)).append(NL);
                            code.append(optimized_literal(i)).append(NL);
                            code.append("iload"+parse_with_under(num_of_reg+1)).append(NL);
                            code.append("iastore" + NL).append(NL);
                            sub_stack_size(1);
                        }
                        sub_stack_size(varg_args_num);
                        code.append("aload"+parse_with_under(num_of_reg)).append(NL);
                    }
                }
                sub_stack_size(1);
                code.append("invokevirtual " + className + "/" + methodName + "(");
                for(Symbol s :table.getParameters(methodName)){
                    String param = getTypeReturnToStr(s.getType());
                    code.append(param);
                }
                code.append(")");
                code.append(getTypeReturnToStr(table.getReturnType(methodName)));
                code.append(NL);
                sub_stack_size(table.getParameters(methodName).size());
                if(!getTypeReturnToStr(table.getReturnType(methodName)).equals("V")){
                    add_stack_size(1);
                }
                if(has_parent_stmt_pop_check( memberCallExpr) && !getTypeReturnToStr(table.getReturnType(methodName)).equals("V")){
                    code.append("pop" + NL);
                    sub_stack_size(1);
                }
                return null;
            }
            if(memberCallExpr.getNumChildren() == 1) {
                sub_stack_size(1);
                code.append("invokevirtual " + className + "/" + methodName + "()");
                if (className.equals(table.getClassName())) {
                    var test= table.getReturnType(methodName);
                    code.append(getTypeReturnToStr(table.getReturnType(methodName))).append(NL);
                    if(has_parent_stmt_pop_check( memberCallExpr) && !getTypeReturnToStr(table.getReturnType(methodName)).equals("V")){
                        code.append("pop" + NL);
                    }
                }else if(has_ancertor_assign(memberCallExpr)!=null){
                    code.append(getTypeToStr(has_ancertor_assign(memberCallExpr))).append(NL);
                    add_stack_size(1);
                }else {
                    code.append("V").append(NL);
                }
                code.append(NL);

                return null;
            }

            var children = memberCallExpr.getChildren();
            sub_stack_size(1);
            code.append("invokevirtual " + className + "/" + methodName + "(");
            for (int i = 1; i<children.size(); i++) {
                t = TypeUtils.getExprType(children.get(i), table, CurrentMethod);
                code.append(getTypeToStr(t));
            }
            code.append(")");
            sub_stack_size(children.size()-1);
            if (className.equals(table.getClassName())) {
                code.append(getTypeReturnToStr(table.getReturnType(methodName))).append(NL);
                if(!getTypeReturnToStr(table.getReturnType(methodName)).equals("V")){
                    add_stack_size(1);
                }
            }else if(has_ancertor_assign(memberCallExpr)!=null){
                code.append(getTypeToStr(has_ancertor_assign(memberCallExpr)));
                add_stack_size(1);

            }else {
                code.append("V");
            }
            code.append(NL);

            if(has_parent_stmt_pop_check( memberCallExpr) && !getTypeReturnToStr(table.getReturnType(methodName)).equals("V")){
                code.append("pop" + NL);
            }



            return null;
        }

    }


    private Void visitMethodCallExpr(JmmNode methodCallExpr, StringBuilder code) {
        var methodName = methodCallExpr.get("funcname");
        var className = table.getClassName();
        var children = methodCallExpr.getChildren();

        if (children.isEmpty()) {
            add_stack_size(1);
            code.append("aload_0" + NL);
            sub_stack_size(1);
            code.append("invokevirtual " + className + "/" + methodName + "()");
            code.append(getTypeReturnToStr(table.getReturnType(methodName)));
            if(!getTypeReturnToStr(table.getReturnType(methodName)).equals("V")){
                add_stack_size(1);
            }
            code.append(NL);

            if(has_parent_stmt_pop_check( methodCallExpr) && !getTypeReturnToStr(table.getReturnType(methodName)).equals("V")){
                code.append("pop" + NL);
            }
            return null;
        }
        int num_of_reg = max_reg_val;
        max_reg_val+=children.size();

        for(int i = children.size(); i>0; i--){
            code.append("astore" + parse_with_under(i+num_of_reg) + NL);
        }
        code.append("aload_0" + NL);
        for(int i = 0; i<children.size(); i++){
            code.append("iload" + parse_with_under(num_of_reg) + NL);
            num_of_reg++;
        }

        code.append("invokevirtual " + className + "/" + methodName + "(");

        for (int i = 0; i<children.size(); i++) {
            var t = TypeUtils.getExprType(children.get(i), table, CurrentMethod);
            code.append(getTypeToStr(t));
        }
        code.append(")");
        code.append(getTypeReturnToStr(table.getReturnType(methodName)));
        code.append(NL);
        if(has_parent_stmt_pop_check( methodCallExpr) && !getTypeReturnToStr(table.getReturnType(methodName)).equals("V")){
            code.append("pop" + NL);
        }

        return null;
    }

    private String getTypeToStr(Type type){
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
    private String getTypeReturnToStr(Type type){
        StringBuilder string=new StringBuilder();
        if(type==null){
            string.append("V");
            return string.toString();
        }
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
        while (parent!=null && parent.getKind().equals("ParenExpr")){
            parent = parent.getParent();
        }
            if ( parent.getKind().equals("BlockStm") || parent.getKind().equals("ExprStmt")){
                return true;
            }else if ( parent.getKind().equals("IfStmt") || parent.getKind().equals("WhileStmt") ){
                if(parent.getJmmChild(0).equals(node)){
                    return false;
                }
                return true;
            }else{
                return false;
            }
    }

    private Boolean has_add_parent(JmmNode node){
        var parent = node.getParent();
        if(parent==null){
            return false;
        }
        while (parent!=null && parent.getKind().equals("ParenExpr")){
            parent = parent.getParent();
        }
        if ( parent.getKind().equals("BinaryExpr") ){
            return parent.get("op").equals("+");
        }else{
            return false;
        }
    }
    public Void add_stack_size(int size){
        stack_size+=size;
        if(stack_size>max_stack_num){
            max_stack_num=stack_size;
        }
        return null;
    }

    public Void sub_stack_size(int size){
        stack_size-=size;
        return null;
    }

    public int get_max_stack_num(){
        return max_stack_num;
    }

    public String parse_with_under(int i){
        if(i>=4){
            return " "+i;
        }else{
            return "_"+i;
        }
    }

    public String optimized_literal(int value){
        if(value>=0 && value<=5){
            return "iconst_" + value + NL;
        } else if (value >= -128 && value <= 127) {
            return "bipush " + value + NL;
        } else if (value >= -32768 && value <= 32767) {
            return "sipush " + value + NL;
        } else {
            return "ldc " + value + NL;

        }
    }

    public String add_optimization(JmmNode node){
        var lft=node.getChildren().get(0);
        var rgt=node.getChildren().get(1);
        StringBuilder s=new StringBuilder();
        if(lft==null || rgt==null){
            return s.toString();
        }
        while(lft.getKind().equals("ParenExpr")){
            lft=lft.getChildren().get(0);
        }
        while(rgt.getKind().equals("ParenExpr")){
            rgt=rgt.getChildren().get(0);
        }
        if(lft.getKind().equals("IntegerLiteral") && rgt.getKind().equals("IntegerLiteral")){
            add_stack_size(1);
            s.append(optimized_literal(Integer.valueOf(lft.get("value"))+Integer.valueOf(rgt.get("value")))).append(NL);
            return s.toString();
        }
        if(lft.getKind().equals("IntegerLiteral") && rgt.getKind().equals("VarRefExpr")){
            var fields = table.getFields();
            String var_name=rgt.get("name");
            var reg_num=currentRegisters.get(var_name);
            String val=lft.get("value");
            int int_val=Integer.valueOf(val);

            if((reg_num==null)){
                for (var field : fields) {
                    if (field.getName().equals(var_name)) {
                        add_stack_size(2);
                        s.append("aload_0" + NL);
                        s.append("getfield " + table.getClassName() + "/" + var_name+" " + getTypeToStr(field.getType()) + NL);
                        s.append(optimized_literal(int_val)).append(NL);
                        s.append("iadd").append(NL);
                        sub_stack_size(1);
                        return s.toString();
                    }
                }
            }
            if(int_val>=-128 && int_val<=127){
                add_stack_size(1);
                s.append("iinc ").append(reg_num).append(" ").append(int_val).append(NL);
                s.append("iload").append(parse_with_under(reg_num)).append(NL);
            }else{
                add_stack_size(2);
                s.append(optimized_literal(int_val)).append(NL);
                s.append("iload").append(parse_with_under(reg_num)).append(NL);
                s.append("iadd").append(NL);
                sub_stack_size(1);
            }
            return s.toString();
        }
        if(rgt.getKind().equals("IntegerLiteral") && lft.getKind().equals("VarRefExpr")){
            String var_name=lft.get("name");
            var fields= table.getFields();
            var reg_num=currentRegisters.get(var_name);
            String val=rgt.get("value");
            int int_val=Integer.valueOf(val);
            if((reg_num==null)){
                for (var field : fields) {
                    if (field.getName().equals(var_name)) {
                        add_stack_size(2);
                        s.append("aload_0" + NL);
                        s.append("getfield " + table.getClassName() + "/" + var_name+" " + getTypeToStr(field.getType()) + NL);
                        s.append(optimized_literal(int_val)).append(NL);
                        s.append("iadd").append(NL);
                        sub_stack_size(1);
                        return s.toString();
                    }
                }
            }
            if(int_val>=-128 && int_val<=127){
                add_stack_size(1);
                s.append("iinc ").append(reg_num).append(" ").append(int_val).append(NL);
                s.append("iload").append(parse_with_under(reg_num)).append(NL);
            }else{
                add_stack_size(2);
                s.append("iload").append(parse_with_under(reg_num)).append(NL);
                s.append(optimized_literal(int_val)).append(NL);
                s.append("iadd").append(NL);
                sub_stack_size(1);
            }
            return s.toString();
        }
        if(lft.getKind().equals("VarRefExpr") && rgt.getKind().equals("VarRefExpr")){

            String var_name1=lft.get("name");
            String var_name2=rgt.get("name");
            var fields= table.getFields();
            var reg_num1=currentRegisters.get(var_name1);
            var reg_num2=currentRegisters.get(var_name2);
            if((reg_num1==null)){
                for (var field : fields) {
                    if (field.getName().equals(var_name1)) {
                        if((reg_num2==null)){
                            for (var field2 : fields) {
                                if (field2.getName().equals(var_name2)) {
                                    add_stack_size(2);
                                    s.append("aload_0" + NL);
                                    s.append("getfield " + table.getClassName() + "/" + var_name1+" " + getTypeToStr(field.getType()) + NL);
                                    s.append("aload_0" + NL);
                                    s.append("getfield " + table.getClassName() + "/" + var_name2+" " + getTypeToStr(field2.getType()) + NL);
                                    s.append("iadd").append(NL);
                                    sub_stack_size(1);
                                    return s.toString();
                                }
                            }
                        }else{
                            add_stack_size(2);
                            s.append("aload_0" + NL);
                            s.append("getfield " + table.getClassName() + "/" + var_name1+" " + getTypeToStr(field.getType()) + NL);
                            s.append("iload").append(parse_with_under(reg_num2)).append(NL);
                            s.append("iadd").append(NL);
                            sub_stack_size(1);
                            return s.toString();
                        }
                    }
                }
            }else{
                if ((reg_num2 == null)) {
                    for (var field : fields) {
                        if (field.getName().equals(var_name2)) {
                            add_stack_size(1);
                            s.append("iload").append(parse_with_under(reg_num1)).append(NL);
                            s.append("aload_0" + NL);
                            s.append("getfield " + table.getClassName() + "/" + var_name2 + " " + getTypeToStr(field.getType()) + NL);
                            s.append("iadd").append(NL);
                            sub_stack_size(1);
                            return s.toString();
                        }
                    }
                } else {
                    add_stack_size(2);
                    s.append("iload").append(parse_with_under(reg_num1)).append(NL);
                    s.append("iload").append(parse_with_under(reg_num2)).append(NL);
                    s.append("iadd").append(NL);
                    sub_stack_size(1);
                    return s.toString();
                }
            }

        }
        if(lft.getKind().equals("VarRefExpr")){
            String var_name=lft.get("name");
            var fields= table.getFields();
            var reg_num=currentRegisters.get(var_name);
            if((reg_num==null)){
                for (var field : fields) {
                    if (field.getName().equals(var_name)) {
                        add_stack_size(1);
                        s.append("aload_0" + NL);
                        s.append("getfield " + table.getClassName() + "/" + var_name+" " + getTypeToStr(field.getType()) + NL);
                        s.append("iadd").append(NL);
                        sub_stack_size(1);
                        return s.toString();
                    }
                }
            }else{
                add_stack_size(1);
                s.append("iload").append(parse_with_under(reg_num)).append(NL);
                s.append("iadd").append(NL);
                sub_stack_size(1);
                return s.toString();
            }
        }
        if(rgt.getKind().equals("VarRefExpr")){
            String var_name=rgt.get("name");
            var fields= table.getFields();
            var reg_num=currentRegisters.get(var_name);
            if((reg_num==null)){
                for (var field : fields) {
                    if (field.getName().equals(var_name)) {
                        add_stack_size(1);
                        s.append("aload_0" + NL);
                        s.append("getfield " + table.getClassName() + "/" + var_name+" " + getTypeToStr(field.getType()) + NL);
                        s.append("iadd").append(NL);
                        sub_stack_size(1);
                        return s.toString();
                    }
                }
            }else{
                add_stack_size(1);
                s.append("iload").append(parse_with_under(reg_num)).append(NL);
                s.append("iadd").append(NL);
                sub_stack_size(1);
                return s.toString();
            }
        }
        if(lft.getKind().equals("IntegerLiteral")){
            String val=lft.get("value");
            int int_val=Integer.valueOf(val);

            add_stack_size(1);
            s.append(optimized_literal(int_val)).append(NL);
            s.append("iadd").append(NL);
            sub_stack_size(1);
            return s.toString();

        }
        if(rgt.getKind().equals("IntegerLiteral")){
            String val=rgt.get("value");
            int int_val=Integer.valueOf(val);

            add_stack_size(1);
            s.append(optimized_literal(int_val)).append(NL);
            s.append("iadd").append(NL);
            sub_stack_size(1);
            return s.toString();
        }
        sub_stack_size(1);
        s.append("iadd").append(NL);
        return s.toString();
    }



}
