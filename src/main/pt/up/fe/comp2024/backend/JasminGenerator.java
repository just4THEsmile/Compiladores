package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";
    private static final String SPACE = " ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    HashMap<String, Descriptor> FieldvarTable = new HashMap<>();

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(PutFieldInstruction.class, this::generateField);
        generators.put(GetFieldInstruction.class, this::getField);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(CallInstruction.class, this::generateCall);
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL).append(NL);

        // TODO: Hardcoded to Object, needs to be expanded

        if (ollirResult.getOllirClass().getSuperClass() != null) {
            code.append(".super ").append(ollirResult.getOllirClass().getSuperClass()).append(NL);

            ArrayList<Field> fields= ollirResult.getOllirClass().getFields();
            for (Field field : fields) {
                code.append(".field ").append(this.getFieldAcess(field)).append(field.getFieldName()).append(SPACE).append(this.getTypeToStr(field.getFieldType())).append(NL);
                break;

            }
            code.append( """
                ;default constructor
                .method public <init>()V
                    aload_0
                    invokespecial""").append(SPACE).append(ollirResult.getOllirClass().getSuperClass());
            code.append("""
                             /<init>()V
                                 return
                             .end method
                              """);

        } else{
            code.append(".super java/lang/Object").append(NL);
            ArrayList<Field> fields= ollirResult.getOllirClass().getFields();
            for (Field field : fields) {
                code.append(".field ").append(this.getFieldAcess(field)).append(field.getFieldName()).append(SPACE).append(this.getTypeToStr(field.getFieldType())).append(NL);
                break;

            }
            code.append( """
                ;default constructor
                .method public <init>()V
                    aload_0
                    invokespecial java/lang/Object/<init>()V
                    return
                .end method
                """);

        }

        // generate a single constructor method

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }

            code.append(generators.apply(method));
        }

        return code.toString();
    }


    private String generateMethod(Method method) {

        // set method
        currentMethod = method;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var methodName = method.getMethodName();

        // TODO: Hardcoded param types and return type, needs to be expanded

        if (method.isStaticMethod()){
            code.append("\n.method ").append(modifier).append("static ").append(methodName);
        }else{
            code.append("\n.method ").append(modifier).append(methodName);
        }
        code.append("(");
        for (var param : method.getParams()) {
            code.append(this.getTypeToStr(param.getType()));
        }
        code.append(")");
        code.append(this.getTypeToStr(method.getReturnType())).append(NL);

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        if (reg == -1){
            return "";
        }

        // TODO: Hardcoded for int type, needs to be expanded
        switch(operand.getType().getTypeOfElement().toString()) {
            case "ARRAYREF", "STRING", "THIS", "OBJECTREF" :
                code.append("astore ").append(reg).append(NL);
                break;
            case "INT32", "BOOLEAN":
                // TODO: Hardcoded to int, needs to be expanded
                code.append("istore ").append(reg).append(NL);
                break;
        }

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        if (reg == -1){
            return "";
        }
        return "iload " + reg + NL;
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case SUB -> "isub";
            case DIV -> "idiv";
            case MUL -> "imul";

            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        // TODO: Hardcoded to int return type, needs to be expanded
        String type = returnInst.getReturnType().getTypeOfElement().toString();
        switch (type){
            case "INT32":
                code.append(generators.apply(returnInst.getOperand()));
                code.append("ireturn").append(NL);
                break;
            case "BOOLEAN":
                code.append(generators.apply(returnInst.getOperand()));
                code.append("ireturn").append(NL);
                break;
            case "STRING":
                code.append(generators.apply(returnInst.getOperand()));
                code.append("areturn").append(NL);
                break;
            case "VOID":
                code.append("return").append(NL);
                break;
            default:
                code.append(generators.apply(returnInst.getOperand()));
                code.append("areturn").append(NL);
        }


        return code.toString();
    }

    private String generateField(PutFieldInstruction field) {
        var code = new StringBuilder();
        code.append("aload_0").append(NL);
        var ops= field.getOperands();
        var firstop = ops.get(0);
        var secondop = ops.get(1);
        var thirdop = ops.get(2);

        code.append(generators.apply(thirdop));
        code.append("putfield ").append(ollirResult.getOllirClass().getClassName()).append("/").append(field.getField().getName()).append(SPACE).append(this.getTypeToStr(field.getField().getType())).append(NL);
        return code.toString();
    }

    private String getFieldAcess(Field field){
        String access = "";
        switch (field.getFieldAccessModifier()){
            case PRIVATE:
                access = "private ";
                break;
            case PROTECTED:
                access = "protected ";
                break;
            case PUBLIC:
                access = "public ";
                break;
            case DEFAULT:
                break;
        }
        if (field.isFinalField()){
            access += "final ";
        }
        if (field.isStaticField()){
            access += "static ";
        }
        return access;
    }

    private String getTypeToStr(Type type){
        switch (type.getTypeOfElement()){
            case VOID:
                return "V";
            case INT32:
                return "I";
            case BOOLEAN:
                return "Z";
            case STRING:
                return "Ljava/lang/String;";
            case ARRAYREF:
                switch (type.toString()){
                    case "INT32[]":
                        return "[I";
                    case "BOOLEAN[]":
                        return "[Z";
                    case "STRING[]":
                        return "[Ljava/lang/String;";
                    default:
                        return null;
                }
            case OBJECTREF:
                return ("L"+this.getClassName(((ClassType)type).getName())) + ";";
            case THIS:
                return "L"+ollirResult.getClass().getSuperclass().getName()+";";
            default:
                // TODO: missing imported classes
                return null;
        }
    }
    private String getField(GetFieldInstruction field) {
        var code = new StringBuilder();
        code.append("aload_0").append(NL);
        code.append("getfield ").append(ollirResult.getOllirClass().getClassName()).append("/").append(field.getField().getName()).append(SPACE).append(this.getTypeToStr(field.getFieldType())).append(NL);
        code.append("dup").append(NL);
        // generate code for loading what's on the right

        // store value in the stack in destination

        return code.toString();
    }
    private String generateCall(CallInstruction call) {
        var code = new StringBuilder();
        // generate code for loading what's on the right
        switch (call.getInvocationType()){
            case invokespecial:
                String method = (((LiteralElement)call.getOperands().get(1)).getLiteral());
                String elemtype= this.getClassName(((ClassType) call.getOperands().get(0).getType()).getName());
                for (Element op : call.getArguments()){
                    code.append(generators.apply(op));
                }
                code.append("invokespecial ").append(elemtype).append("/").append(this.remove_quotes(method));

                //arguments
                code.append("(");
                for (Element op : call.getArguments()){
                    code.append(this.getTypeToStr(op.getType()));
                }
                code.append(")");
                code.append(this.getTypeToStr(call.getReturnType())).append(NL);
                break;
            case invokestatic:
                var elemt= ( (Operand) call.getOperands().get(0)).getName();
                method = (((LiteralElement) call.getOperands().get(1)).getLiteral());
                for (Element op : call.getArguments()){
                    code.append(generators.apply(op));
                }
                code.append("invokestatic ").append(elemt).append("/").append(this.remove_quotes(method));
                code.append("(");
                for (Element op : call.getArguments()){
                    code.append(this.getTypeToStr(op.getType()));
                }
                code.append(")");
                code.append(this.getTypeToStr(call.getReturnType())).append(NL);



                break;
            case invokevirtual:

                break;
            case NEW:
                if (call.getReturnType().getTypeOfElement() == ElementType.OBJECTREF){
                    for (Element op : call.getArguments()){
                        code.append(this.getTypeToStr(op.getType()));
                    }
                    var obj=this.getClassName(((ClassType)call.getReturnType()).getName());
                    code.append("new ").append(obj).append(NL);
                    code.append("dup").append(NL);
                }else{
                    for (Element op : call.getArguments()){
                        code.append(generators.apply(op));
                    }
                    code.append("newarray ");
                    switch (call.getReturnType().getTypeOfElement()){
                        case INT32:
                            code.append("int");
                            break;
                        case BOOLEAN:
                            code.append("boolean");
                            break;
                        case STRING:
                            code.append("java/lang/String");
                            break;
                    }
                    code.append(NL);
                }

                break;
            case ldc:
                break;
            case arraylength:
                break;
            default:
                throw new NotImplementedException(call.getInvocationType());

        }



        return code.toString();
    }

    String getClassName(String className){
        if (className.equals("this")){
            return ollirResult.getOllirClass().getClassName();
        }
        for(String imported : ollirResult.getOllirClass().getImports()){
            if (imported.contains(className)){
                return imported;
            }
        }
        return className.substring(className.lastIndexOf(".")+1);
    }

    private String remove_quotes(String str){
        return str.substring(1,str.length()-1);
    }

}
