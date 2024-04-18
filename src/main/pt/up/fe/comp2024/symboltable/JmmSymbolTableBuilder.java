package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static pt.up.fe.comp2024.ast.Kind.*;

public class JmmSymbolTableBuilder {


    public static JmmSymbolTable build(JmmNode root) {
        var importsDecl = root.getChildren("ImportDecl");
        var classDecl = root.getChildren("ClassDecl").get(0);
        SpecsCheck.checkArgument(CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");

        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);
        var fields = buildFields(classDecl);
        var Imports =buildImports(importsDecl);
        var parent = buildParent(classDecl);
        var varArgs = buildVarArgs(classDecl);

        return new JmmSymbolTable(className, methods, returnTypes, params,fields, locals,Imports,varArgs,parent);
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {

        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> {
                    if(method.getChildren("Type").size()>0){

                        if (method.getChildren("Type").get(0).getChildren("TypeArray").size()>0){
                            map.put(method.get("name"), new Type(method.getChildren("Type").get(0).getChildren("TypeArray").get(0).get("name"), true));
                    }else {
                        map.put(method.get("name"), new Type(method.getChildren("Type").get(0).get("name"), false));
                    }
                    }else{
                        map.put(method.get("name"), new Type("void", false));
                    }

                });
        classDecl.getChildren("MainMethodDecl").stream()
                .forEach(method -> map.put("main", new Type("void", false)));

        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {

        Map<String, List<Symbol>> map = new HashMap<>();

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method ->
                            {   List<Symbol> symbols = new ArrayList<Symbol>();
                                if(method.getChildren("Paramlist").size()>0) {
                                    method.getChildren("Paramlist").get(0).getChildren("Param").stream()
                                            .forEach(param -> {
                                                Type type = new Type(null, false);
                                                // in case of an array
                                                if (param.getChildren("Type").get(0).getChildren("TypeArray").size() > 0) {
                                                    type = new Type(param.getChildren("Type").get(0).getChildren("TypeArray").get(0).get("name"), true);
                                                } else {
                                                    type = new Type(param.getChildren("Type").get(0).get("name"), false);
                                                }

                                                symbols.add(new Symbol(type, param.get("name")));
                                            });

                                }
                                try{ // in case of varargs
                                    symbols.add(new Symbol(new Type("_varargs",true),method.getChildren("Paramlist").get(0).get("val")));
                                }catch (Exception e){
                                    //do nothing
                                }
                                    map.put(method.get("name"), symbols);


                            }
                        );
        classDecl.getChildren("MainMethodDecl").stream()
                .forEach(method ->{
                        try{
                        map.put("main",Arrays.asList(new Symbol (new Type("String",true),method.get("arg"))));
                        }catch (NullPointerException e){
                            map.put("main",new ArrayList<Symbol>());
                        }

                });

        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {

        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> {


                    map.put(method.get("name"), getLocalsList(method));



                });
        classDecl.getChildren("MainMethodDecl").stream()
                .forEach(method -> {


                    map.put("main", getLocalsList(method));



                });

        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {
        List<String> methods ;
        List<String> rList = new ArrayList<String>();
        methods = classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
        rList.addAll(methods);
        if(classDecl.getChildren("MainMethodDecl").size()>0){


            rList.add("main");
        }

        return rList;
    }


    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        List<Symbol> locals= new ArrayList<Symbol>();
        if(methodDecl.getChildren("VarDecl").size()>0){
            methodDecl.getChildren("VarDecl").stream()
                    .forEach(varDecl -> {
                        Type type = new Type(null, false);
                        if (varDecl.getChildren("Type").size() > 0 &&
                                varDecl.getChildren("Type").get(0).getChildren("TypeArray").size() > 0) {
                            // Acessar o nome do tipo array
                            type = new Type(varDecl.getChildren("Type").get(0).getChildren("TypeArray").get(0).get("name"), true);
                        } else if (varDecl.getChildren("Type").size() > 0) {
                            // Acessar o nome do tipo não array
                            type = new Type(varDecl.getChildren("Type").get(0).get("name"), false);
                        }

                        locals.add(new Symbol(type ,varDecl.get("name")));
                    });
        }
        return locals;
    }
    private static List<Symbol> buildFields(JmmNode classDecl) {
        var symbols = new ArrayList<Symbol>();


        classDecl.getChildren(VAR_DECL).stream()
                .forEach(varDecl -> {

                    Type type = new Type("Typeee", false);
                    if(varDecl.getChildren("Type").size()>0) {
                        // in case of an array
                        if (varDecl.getChildren("Type").get(0).getChildren("TypeArray").size() > 0) {
                            type = new Type(varDecl.getChildren("Type").get(0).getChildren("TypeArray").get(0).get("name"), true);
                        } else {
                            type = new Type(varDecl.getChildren("Type").get(0).get("name"), false);
                        }
                    }
                    symbols.add(new Symbol(type ,varDecl.get("name")));


                });

        return symbols;
    }

    private static List<String> buildImports(List<JmmNode> importsDecl) {
        if(importsDecl.size()==0){
            return new ArrayList<String>();
        }
        var imports = new ArrayList<String>();
        importsDecl.forEach(importDecl -> {

                    imports.add(importDecl.get("value"));
                });


        return imports;
    }

    private static String buildParent(JmmNode classDecl) {
        try {
            var parent = classDecl.get("parent");
            // Access the parent value
            return parent;
        } catch (NullPointerException e) {
            // Handle the case where the parent value doesn't exist
            return null; // or throw new IllegalStateException("Parent value is not present");
        }
    }

    public List<String> getSuperClasses(JmmNode root) {
        List<String> superClasses = new ArrayList<>();
        JmmNode classDecl = root.getChildren("ClassDecl").get(0); // Assuming there's only one class declaration
        if (classDecl.getChildren("Extends").size() > 0) {
            String superClass = classDecl.getChildren("Extends").get(0).get("name");
            superClasses.add(superClass);
        }
        return superClasses;
    }
    private static Map<String, Boolean> buildVarArgs(JmmNode classDecl) {
        Map<String, Boolean> map = new HashMap<>();
        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> {
                    if (method.getChildren("_varargs").size()==1){
                        map.put(method.get("name"),true);
                    }else{
                        map.put(method.get("name"),false);
                    }
                });
        return map;
    }
}
