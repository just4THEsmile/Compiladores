package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
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

        var classDecl = root.getJmmChild(0);
        SpecsCheck.checkArgument(CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");

        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);
        var fields = buildFields(classDecl);
        var Imports =buildImports(classDecl);

        return new JmmSymbolTable(className, methods, returnTypes, params,fields, locals,Imports);
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> {
                    if(method.getChildren("Type").size()>0){

                        if (method.getChildren("Type").get(0).getChildren("Type").size()>0){
                            map.put(method.get("name"), new Type(method.getChildren("Type").get(0).getChildren("Type").get(0).get("name"), true));
                    }else {
                        map.put(method.get("name"), new Type(method.getChildren("Type").get(0).get("name"), false));
                    }
                    }

                });
        classDecl.getChildren("MainMethodDecl").stream()
                .forEach(method -> map.put("main", new Type("Void", false)));

        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {

        Map<String, List<Symbol>> map = new HashMap<>();

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method ->
                        {   List<Symbol> symbols = new ArrayList<Symbol>();
                            if (method.getChildren().size()>=2) {
                                JmmNode param = method.getJmmChild(1);
                                if (param.getKind().equals("Params")) {
                                    Type type = new Type("Typeee", false);
                                    if (method.getJmmChild(0).getChildren().size()>0) {
                                        type = new Type(method.getJmmChild(0).getJmmChild(0).get("name"), false);
                                    }else{
                                        type = new Type(method.getJmmChild(0).get("name"), false);
                                    }

                                    String kind = param.getKind();
                                    while (kind.equals("Params") && param.getChildren().size()>=2) {
                                        symbols.add(new Symbol(type, param.get("name")));
                                        param = param.getJmmChild(1);
                                        type = new Type(param.getJmmChild(0).get("name"), false);
                                        kind = param.getKind();
                                    }
                                    type= new Type(param.getJmmChild(0).get("name"), false);
                                    symbols.add(new Symbol(type, param.get("name")));
                                }
                            map.put(method.get("name"), symbols);
                            }
                        }
                        );
        classDecl.getChildren("MainMethodDecl").stream()
                .forEach(method -> map.put("main", Arrays.asList(new Symbol (new Type("String",true),"args"))));

        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

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
        // TODO: Simple implementation that needs to be expanded

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(intType, varDecl.get("name")))
                .toList();
    }
    private static List<Symbol> buildFields(JmmNode classDecl) {
        var symbols = new ArrayList<Symbol>();


        classDecl.getChildren(VAR_DECL).stream()
                .forEach(varDecl -> {

                    Type type = new Type("Typeee", false);
                    if(varDecl.getChildren("Type").size()>0) {
                        // in case of an array
                        if (varDecl.getChildren("Type").get(0).getChildren("Type").size() > 0) {
                            type = new Type(varDecl.getChildren("Type").get(0).getChildren("Type").get(0).get("name"), true);
                        } else {
                            type = new Type(varDecl.getChildren("Type").get(0).get("name"), false);
                        }
                    }
                    symbols.add(new Symbol(type ,varDecl.get("name")));


                });

        return symbols;
    }

    private static List<String> buildImports(JmmNode classDecl) {
        var imports = new ArrayList<String>();
        classDecl.getChildren("ImportDecl").stream()
                .forEach(importDecl -> {

                    imports.add(importDecl.get("name"));
                });


        return imports;
    }

}
